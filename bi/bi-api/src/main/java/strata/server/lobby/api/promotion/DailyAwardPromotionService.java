package strata.server.lobby.api.promotion;

import com.yazino.platform.Platform;
import strata.server.lobby.api.promotion.message.TopUpRequest;

import java.math.BigDecimal;

public interface DailyAwardPromotionService {
    void awardDailyTopUp(final TopUpRequest topUpRequest);

    TopUpResult getTopUpResult(BigDecimal playerId, final Platform platform);
}
