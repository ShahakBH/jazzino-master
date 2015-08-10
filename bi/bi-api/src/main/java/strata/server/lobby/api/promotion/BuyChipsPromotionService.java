package strata.server.lobby.api.promotion;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.joda.time.DateTime;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BuyChipsPromotionService {
    Map<Currency, List<PaymentOption>> getBuyChipsPaymentOptionsFor(
            BigDecimal playerId, Platform platform);

    InGameMessage getInGameMessageFor(BigDecimal playerId);
    InGameMessage getInGameMessageFor(BigDecimal playerId, Platform platform);

    PaymentOption getPaymentOptionFor(BigDecimal playerId,
                                      Long promotionId,
                                      PaymentPreferences.PaymentMethod paymentMethod,
                                      String paymentOptionId);

    PaymentOption getDefaultPaymentOptionFor(String paymentOptionId, Platform platform);

    PaymentOption getDefaultFacebookPaymentOptionFor(final String paymentOptionId);

    void logPlayerReward(BigDecimal playerId,
                         Long promotionId,
                         PaymentPreferences.PaymentMethod paymentMethod,
                         String paymentOptionId,
                         DateTime awardDate);

    void logPlayerReward(BigDecimal playerId,
                         Long promotionId,
                         BigDecimal defaultChips,
                         BigDecimal promoChips,
                         PaymentPreferences.PaymentMethod paymentMethod,
                         DateTime awardDate);

    Boolean hasPromotion(BigDecimal playerId, Platform platform);

}
