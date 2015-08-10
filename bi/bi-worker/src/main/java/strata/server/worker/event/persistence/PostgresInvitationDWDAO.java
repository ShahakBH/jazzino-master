package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.InvitationEvent;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.Collection;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresInvitationDWDAO extends PostgresDWDAO<InvitationEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE INVITATIONS "
            + "SET STATUS = stage.STATUS, REWARD = stage.REWARD, UPDATED_TS = stage.UPDATED_TS "
            + "FROM STG_INVITATIONS stage "
            + "WHERE INVITATIONS.PLAYER_ID = stage.PLAYER_ID"
            + " AND INVITATIONS.RECIPIENT_IDENTIFIER = stage.RECIPIENT_IDENTIFIER"
            + " AND INVITATIONS.INVITED_FROM = stage.INVITED_FROM";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO INVITATIONS "
            + "SELECT stage.* FROM STG_INVITATIONS stage "
            + "LEFT JOIN INVITATIONS target ON "
            + " stage.PLAYER_ID = target.PLAYER_ID "
            + " AND stage.RECIPIENT_IDENTIFIER = target.RECIPIENT_IDENTIFIER "
            + " AND stage.INVITED_FROM = target.INVITED_FROM "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_INVITATIONS";


    PostgresInvitationDWDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresInvitationDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createInsertStatementFor(final Collection<InvitationEvent> events) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_INVITATIONS",
                "PLAYER_ID", "RECIPIENT_IDENTIFIER", "INVITED_FROM", "STATUS", "REWARD",
                "CREATED_TS", "UPDATED_TS", "GAME_TYPE", "SCREEN_SOURCE");
        for (InvitationEvent event : events) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(event.getIssuingPlayerId()),
                    sqlString(event.getRecipientIdentifier()),
                    sqlString(event.getSource().name()),
                    sqlString(event.getStatus()),
                    sqlBigDecimal(event.getReward()),
                    sqlTimestamp(nowIfNull(event.getCreatedTime())),
                    sqlTimestamp(nowIfNull(event.getUpdatedTime())),
                    sqlString(event.getGameType()),
                    sqlString(event.getScreenSource()));
        }

        return insertBuilder.toSql();
    }

    private DateTime nowIfNull(final DateTime instant) {
        if (instant == null) {
            return new DateTime();
        }
        return instant;
    }

    @Override
    protected String[] getBatchUpdates(final List<InvitationEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }
}
