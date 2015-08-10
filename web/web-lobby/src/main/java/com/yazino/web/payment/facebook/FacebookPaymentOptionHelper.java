package com.yazino.web.payment.facebook;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import com.yazino.web.payment.PaymentOptionIdTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.FACEBOOK;

/**
 * TODO RENAME ??
 */
@Service
public class FacebookPaymentOptionHelper implements PaymentOptionIdTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookPaymentOptionHelper.class);

    // moved from CashierController
    public void modifyPaymentOptionIdsIn(String gameType, final Map<Currency, List<PaymentOption>> prefilteredPaymentOptionsMap) {
        for (Currency currency : prefilteredPaymentOptionsMap.keySet()) {
            final List<PaymentOption> paymentOptions = prefilteredPaymentOptionsMap.get(currency);
            for (PaymentOption paymentOption : paymentOptions) {
                String transformedId = transformPaymentOptionId(gameType, paymentOption, FACEBOOK);
                paymentOption.setId(transformedId);
            }
        }
    }

    @Override
    public String transformPaymentOptionId(String gameType, PaymentOption paymentOption, final PaymentPreferences.PaymentMethod paymentMethod) {
        LOG.debug("transforming payment option id for game '%s' and id '%s'", gameType, paymentOption.getId());

        if (!paymentOption.getId().startsWith("option")) {
            //not sure what to do with this so default it
            return paymentOption.getId();
        }
        final StringBuilder reformattedId = new StringBuilder(paymentOption.getId().substring("option".length()).toLowerCase());
        reformattedId.append("_").append(paymentOption.getAmountRealMoneyPerPurchase().intValue());
        String amountOfChips;
        final PromotionPaymentOption facebookPromo = paymentOption.getPromotion(paymentMethod);
        if (facebookPromo != null) {
            amountOfChips = facebookPromo.getPromotionChipsPerPurchase().divide(new BigDecimal("1000")).toPlainString();
        } else {
            amountOfChips = paymentOption.getNumChipsPerPurchase().divide(new BigDecimal("1000")).toPlainString();
        }
        reformattedId.append("_buys_").append(amountOfChips).append("k");
        if (facebookPromo != null) {
            reformattedId.append("_").append(facebookPromo.getPromoId());
        }
        return reformattedId.toString();
    }
}
