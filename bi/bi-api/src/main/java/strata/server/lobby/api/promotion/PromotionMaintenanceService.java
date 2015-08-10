package strata.server.lobby.api.promotion;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Allows promotions to be created etc.
 */
public interface PromotionMaintenanceService {

    Long create(Promotion promo);

    void update(Promotion promo);

    void delete(Long promoId);

    // calls to this service may need to be batched if list is long
    void addPlayersTo(Long promoId, Set<BigDecimal> playerIds);

    DailyAwardConfig getDefaultDailyAwardConfiguration();
}
