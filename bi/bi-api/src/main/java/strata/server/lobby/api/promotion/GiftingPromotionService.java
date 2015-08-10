package strata.server.lobby.api.promotion;

import com.yazino.platform.gifting.AppToUserGift;

import java.math.BigDecimal;
import java.util.List;

public interface GiftingPromotionService {
    List<AppToUserGift> getGiftingPromotions(BigDecimal playerId);

    boolean logPlayerReward(BigDecimal playerId, Long promotionId, final BigDecimal sessionId);
}
