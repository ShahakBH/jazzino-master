package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.PlayerLevelEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresPlayerLevelDWDAO extends PostgresDWDAO<PlayerLevelEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE PLAYER_LEVEL "
            + "SET GAME_TYPE = stage.GAME_TYPE, LEVEL = stage.LEVEL "
            + "FROM STG_PLAYER_LEVEL stage "
            + "WHERE PLAYER_LEVEL.PLAYER_ID = stage.PLAYER_ID "
            + "AND PLAYER_LEVEL.GAME_TYPE = stage.GAME_TYPE";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO PLAYER_LEVEL "
            + "SELECT stage.* FROM STG_PLAYER_LEVEL stage "
            + "LEFT JOIN PLAYER_LEVEL target ON stage.PLAYER_ID = target.PLAYER_ID "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM stg_player_level";

    //CGlib constructor
    public PostgresPlayerLevelDWDAO() {
        super(null);
    }

    @Autowired
    public PostgresPlayerLevelDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<PlayerLevelEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };

    }

    private String createInsertStatementFor(final List<PlayerLevelEvent> playerLevelEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_PLAYER_LEVEL", "PLAYER_ID", "GAME_TYPE", "LEVEL");
        for (PlayerLevelEvent playerLevelEvent : playerLevelEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(new BigDecimal(playerLevelEvent.getPlayerId())),
                    sqlString(playerLevelEvent.getGameType()),
                    sqlString(playerLevelEvent.getLevel()));
        }

        return insertBuilder.toSql();
    }

}
