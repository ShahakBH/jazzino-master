package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.promotion.PromoRewardEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresPromotionRewardDWDAO extends PostgresDWDAO<PromoRewardEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE PROMO_REWARD "
            + "SET promo_id = stage.PROMO_ID "
            + "FROM STG_PROMO_REWARD stage "
            + "WHERE PROMO_REWARD.PLAYER_ID = stage.PLAYER_ID "
            + "AND~ PROMO_REWARD.PROMO_ID = stage.PROMO_ID "
            + "AND PROMO_REWARD.ACTIVITY_TS= stage.ACTIVITY_TS";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO PROMO_REWARD "
            + "SELECT stage.* FROM STG_PROMO_REWARD stage "
            + "LEFT JOIN PROMO_REWARD target ON "
            + "stage.PLAYER_ID = target.PLAYER_ID and "
            + "stage.PROMO_ID = target.PROMO_ID and "
            + "stage.ACTIVITY_TS = target.ACTIVITY_TS "
            + "WHERE target.PLAYER_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM stg_promo_reward";

    //CGlib constructor
    PostgresPromotionRewardDWDAO() {
        super(null);
    }

    @Autowired
    public PostgresPromotionRewardDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<PromoRewardEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };
    }

    private String createInsertStatementFor(final List<PromoRewardEvent> promoRewardEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_PROMO_REWARD", "PLAYER_ID", "PROMO_ID", "ACTIVITY_TS");
        for (PromoRewardEvent promoRewardEvent : promoRewardEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(promoRewardEvent.getPlayerId()),
                    sqlLong(promoRewardEvent.getPromoId()),
                    sqlTimestamp(promoRewardEvent.getActivityTime()));
        }

        return insertBuilder.toSql();
    }

}
