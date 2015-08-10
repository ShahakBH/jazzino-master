package com.yazino.web.payment.itunes;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.payment.PaymentOptionIdTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class IosPaymentOptionIdTransformer implements PaymentOptionIdTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(IosPaymentOptionIdTransformer.class);


    @Override
    public String transformPaymentOptionId(String gameType, PaymentOption paymentOption, final PaymentPreferences.PaymentMethod paymentMethod) {
        LOG.debug("transforming payment option id for game {} and id {}", gameType, paymentOption.getId());

        if (!paymentOption.getId().startsWith("IOS")) {
            //not sure what to do with this so default it
            return paymentOption.getId();
        }
        String amountOfChips;
        final PromotionPaymentOption promo = paymentOption.getPromotion(paymentMethod);
        if (promo != null) {
            amountOfChips = promo.getPromotionChipsPerPurchase().divide(new BigDecimal("1000")).toPlainString();
        } else {
            amountOfChips = paymentOption.getNumChipsPerPurchase().divide(new BigDecimal("1000")).toPlainString();
        }
        String promoString = "";
        if (promo != null) {
            BigDecimal percent = calculatePromotionPercent(paymentOption.getNumChipsPerPurchase(), promo.getPromotionChipsPerPurchase());
            if (BigDecimal.ZERO.compareTo(percent) < 0) {
                promoString = "_P" + percent.intValue();
            }
        }

        final String gameIdentifier;
        if ("BLACKJACK".equals(gameType)) {
            gameIdentifier = "BJ_";
        } else {
            gameIdentifier = "";
        }
        String format = String.format("%s%s_BUYS_%s%s",
                                      gameIdentifier,
                paymentOption.getCurrencyCode() + paymentOption.getAmountRealMoneyPerPurchase().intValue(),
                amountOfChips + "K",
                promoString);

        return format;
    }

    private static BigDecimal calculatePromotionPercent(final BigDecimal normalChips,
                                                        final BigDecimal promoChips) {
        final boolean differ = normalChips.compareTo(promoChips) < 0;
        if (differ) {
            // assuming promochips is always bigger than normal chips
            final BigDecimal extra = promoChips.subtract(normalChips);
            final BigDecimal fraction = extra.divide(normalChips);
            return fraction.multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

}
