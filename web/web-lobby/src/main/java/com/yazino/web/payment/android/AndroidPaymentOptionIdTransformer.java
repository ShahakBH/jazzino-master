package com.yazino.web.payment.android;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.payment.PaymentOptionIdTransformer;
import com.yazino.web.payment.googlecheckout.v3.AndroidProductFactory;
import com.yazino.web.payment.googlecheckout.v3.AndroidStoreProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class AndroidPaymentOptionIdTransformer implements PaymentOptionIdTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidPaymentOptionIdTransformer.class);

    private final AndroidProductFactory androidProductFactory;

    @Autowired
    public AndroidPaymentOptionIdTransformer(AndroidProductFactory androidProductFactory) {
        this.androidProductFactory = androidProductFactory;
    }

    /*
     * Transforms Yazino PaymentOption ids into a Amazon/Google product id.
     */
    @Override
    public String transformPaymentOptionId(String gameType, PaymentOption paymentOption, PaymentPreferences.PaymentMethod paymentMethod) {
        AndroidStoreProduct product = androidProductFactory.getProductFor(gameType, paymentOption, paymentMethod);
        if (product == null) {
            LOG.debug("Failed to find android product for game {} and option: {}", gameType, paymentOption);
            return null;
        }
        return product.getProductId();
    }
}
