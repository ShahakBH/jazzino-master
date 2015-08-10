package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.Collection;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresPlayerReferrerDWDAO extends PostgresDWDAO<PlayerReferrerEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE LOBBY_USER "
            + "SET REGISTRATION_GAME_TYPE = COALESCE(stage.REGISTRATION_GAME_TYPE, LOBBY_USER.REGISTRATION_GAME_TYPE), "
            + " REGISTRATION_REFERRER = COALESCE(stage.REGISTRATION_REFERRER, LOBBY_USER.REGISTRATION_REFERRER), "
            + " REGISTRATION_PLATFORM = COALESCE(stage.REGISTRATION_PLATFORM, LOBBY_USER.REGISTRATION_PLATFORM) "
            + "FROM STG_LOBBY_USER stage "
            + "WHERE LOBBY_USER.PLAYER_ID = stage.PLAYER_ID"
            + " AND (stage.REGISTRATION_REFERRER <> 'INVITE' OR LOBBY_USER.REGISTRATION_REFERRER IS NULL OR LOBBY_USER.REGISTRATION_REFERRER = '')";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO LOBBY_USER "
            + "SELECT stage.* FROM STG_LOBBY_USER stage "
            + "LEFT JOIN LOBBY_USER target ON stage.PLAYER_ID = target.PLAYER_ID "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_LOBBY_USER";

    PostgresPlayerReferrerDWDAO() { //CGLIB
        super(null);
    }

    @Autowired
    public PostgresPlayerReferrerDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<PlayerReferrerEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }

    private String createInsertStatementFor(final Collection<PlayerReferrerEvent> events) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_LOBBY_USER",
                "PLAYER_ID", "REGISTRATION_PLATFORM", "REGISTRATION_GAME_TYPE", "REGISTRATION_REFERRER");
        for (PlayerReferrerEvent event : events) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(event.getPlayerId()),
                    sqlString(event.getPlatform()),
                    sqlString(event.getGameType()),
                    sqlString(event.getRef()));
        }

        return insertBuilder.toSql();
    }

}

