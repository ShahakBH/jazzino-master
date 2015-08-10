package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.PlayerEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlTimestamp;

@Repository
public class PostgresPlayerDWDAO extends PostgresDWDAO<PlayerEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE LOBBY_USER "
            + "SET ACCOUNT_ID = COALESCE(stage.ACCOUNT_ID, LOBBY_USER.ACCOUNT_ID), "
            + " REG_TS= COALESCE(stage.REG_TS, LOBBY_USER.REG_TS) "
            + "FROM STG_LOBBY_USER stage "
            + "WHERE LOBBY_USER.PLAYER_ID = stage.PLAYER_ID";


    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO LOBBY_USER "
            + "SELECT stage.* FROM STG_LOBBY_USER stage "
            + "LEFT JOIN LOBBY_USER target ON stage.PLAYER_ID = target.PLAYER_ID "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_LOBBY_USER";


    PostgresPlayerDWDAO() { // CGLIB
        super(null);
    }

    @Autowired
    public PostgresPlayerDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createInsertStatementFor(final List<PlayerEvent> playerEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_LOBBY_USER", "PLAYER_ID", "REG_TS", "ACCOUNT_ID");
        for (PlayerEvent playerEvent : playerEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(playerEvent.getPlayerId()),
                    sqlTimestamp(playerEvent.getTsCreated()),
                    sqlBigDecimal(playerEvent.getAccountId()));
        }

        return insertBuilder.toSql();
    }

    @Override
    protected String[] getBatchUpdates(final List<PlayerEvent> events) {
        return new String[]{createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }
}
