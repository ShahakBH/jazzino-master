package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.BonusCollectedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlTimestamp;

@Repository
public class PostgresBonusCollectedEventDao extends PostgresDWDAO<BonusCollectedEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE LOCKOUT_BONUS SET LAST_BONUS_TS = stage.LAST_BONUS_TS "
            + "FROM STG_LOCKOUT_BONUS stage "
            + "WHERE LOCKOUT_BONUS.PLAYER_ID = stage.PLAYER_ID";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO LOCKOUT_BONUS "
            + "SELECT stage.* FROM STG_LOCKOUT_BONUS stage "
            + "LEFT JOIN LOCKOUT_BONUS target ON stage.PLAYER_ID = target.PLAYER_ID "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_LOCKOUT_BONUS";

    @Autowired
    protected PostgresBonusCollectedEventDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    //CGLIB
    protected PostgresBonusCollectedEventDao() {
        super(null);
    }

    @Override
    protected String[] getBatchUpdates(final List<BonusCollectedEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }

    private String createInsertStatementFor(final List<BonusCollectedEvent> events) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_LOCKOUT_BONUS", "PLAYER_ID", "LAST_BONUS_TS");
        for (BonusCollectedEvent giftCollectedEvent : events) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(giftCollectedEvent.getPlayerId()),
                    sqlTimestamp(giftCollectedEvent.getCollected()));
        }

        return insertBuilder.toSql();
    }


}
