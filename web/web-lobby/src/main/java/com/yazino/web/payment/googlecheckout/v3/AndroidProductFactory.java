package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

@Component
public class AndroidProductFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidProductFactory.class);
    private ChipBundleResolver chipBundleResolver;

    @Autowired
    public AndroidProductFactory(ChipBundleResolver chipBundleResolver) {
        this.chipBundleResolver = chipBundleResolver;
    }

    public AndroidStoreProduct getProductFor(String gameType, PaymentOption paymentOption, PaymentPreferences.PaymentMethod paymentMethod) {
        LOG.debug("Looking up android product for option: {}", paymentOption);
        ChipBundle chipBundle = findChipBundleFor(gameType, paymentOption, paymentMethod);
        if (chipBundle == null) {
            return null;
        }
        AndroidStoreProduct product = new AndroidStoreProduct(chipBundle.getProductId(), chipBundle.getDefaultChips());
        if (chipBundle.getDefaultChips().longValue() != chipBundle.getChips().longValue()) {
            product.setPromoChips(chipBundle.getChips());
        }
        return product;
    }

    private ChipBundle findChipBundleFor(String gameType, PaymentOption option, final PaymentPreferences.PaymentMethod paymentMethod) {
        ChipBundle bundle = null;
        if (option.hasPromotion(paymentMethod)) {
            PromotionPaymentOption promotion = option.getPromotion(paymentMethod);
            String chipBundleKey = buildPromotionChipBundleKey(option, promotion);
            bundle = chipBundleResolver.findChipBundleFor(gameType, chipBundleKey);
            if (bundle == null) {
                // The chip bundle may be null if the package hasn't been overridden in the promotion or if the
                // player is in the control group.
                // This whole promotion stuff is bit of a mess and should be reconsidered.
                LOG.error("Ignoring promotion[{}], chipBundleKey[{}] since no matching bundle found", promotion.getPromoId(), chipBundleKey);
            }
        }
        if (bundle == null) {
            bundle = findDefaultChipBundle(gameType, option);
            if (bundle == null) {
                LOG.error("Failed to find android chip bundle for option {}", option);
            }
        }
        return bundle;
    }

    private ChipBundle findDefaultChipBundle(String gameType, PaymentOption paymentOption) {
        return chipBundleResolver.findChipBundleFor(
                gameType,
                "" + paymentOption.getNumChipsPerPurchase().longValue());
    }

    private String buildPromotionChipBundleKey(final PaymentOption paymentOption, final PromotionPaymentOption promotion) {
        if (promotion.getPromotionChipsPerPurchase() == null
                || paymentOption.getNumChipsPerPurchase().compareTo(promotion.getPromotionChipsPerPurchase()) == 0) {
            return paymentOption.getNumChipsPerPurchase().longValue() + "";
        } else {
            return paymentOption.getNumChipsPerPurchase().longValue()
                    + "-"
                    + promotion.getPromotionChipsPerPurchase().longValue();
        }
    }
}
