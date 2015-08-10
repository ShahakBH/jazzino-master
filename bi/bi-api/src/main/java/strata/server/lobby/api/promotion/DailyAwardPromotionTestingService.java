package strata.server.lobby.api.promotion;

import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;

import java.math.BigDecimal;

public interface DailyAwardPromotionTestingService {

    PlayerPromotionStatus getPlayerPromotionStatus(final BigDecimal playerID);

    PlayerPromotionStatus setDailyAwardStatus(PlayerPromotionStatus playerPromotionStatus);

}
