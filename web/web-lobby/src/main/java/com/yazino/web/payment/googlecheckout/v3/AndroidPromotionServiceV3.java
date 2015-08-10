package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Queries available android products for player.
 */
@Service
public class AndroidPromotionServiceV3 {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidPromotionServiceV3.class);

    private final BuyChipsPromotionService buyChipsPromotionService;
    private final AndroidProductFactory androidProductFactory;

    @Autowired
    public AndroidPromotionServiceV3(BuyChipsPromotionService buyChipsPromotionService,
                                     AndroidProductFactory androidProductFactory) {
        notNull(buyChipsPromotionService);
        notNull(androidProductFactory);
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.androidProductFactory = androidProductFactory;
    }

    public AndroidStoreProducts getAvailableProducts(BigDecimal playerId, Platform platform, String gameType) {
        notNull(gameType, "gameType");
        Map<Currency, List<PaymentOption>> paymentOptions = buyChipsPromotionService.getBuyChipsPaymentOptionsFor(playerId, platform);
        return selectProductsForUSD(gameType, paymentOptions);
    }

    private AndroidStoreProducts selectProductsForUSD(String gameType, Map<Currency, List<PaymentOption>> paymentOptionsPerCurrency) {
        if (paymentOptionsPerCurrency == null) {
            LOG.error("No ANDROID payment options found");
            return new AndroidStoreProducts();
        }
        List<PaymentOption> paymentOptionsUSD = paymentOptionsPerCurrency.get(Currency.USD);
        if (paymentOptionsUSD == null || paymentOptionsUSD.isEmpty()) {
            LOG.error("No ANDROID payment options found for USD");
            return new AndroidStoreProducts();
        }
        return selectProducts(gameType, paymentOptionsUSD);
    }

    private AndroidStoreProducts selectProducts(String gameType, List<PaymentOption> paymentOptions) {
        AndroidStoreProducts products = new AndroidStoreProducts();
        if (paymentOptions != null) {
            for (PaymentOption paymentOption : paymentOptions) {
                AndroidStoreProduct product = androidProductFactory.getProductFor(gameType, paymentOption, GOOGLE_CHECKOUT);
                if (product != null) {
                    LOG.debug("Adding product: {}", product);
                    products.addProduct(product);
                    if (paymentOption.hasPromotion(GOOGLE_CHECKOUT)) {
                        products.setPromoId(paymentOption.getPromotion(GOOGLE_CHECKOUT).getPromoId());
                    }
                }
            }
        } else {
            LOG.error("No payment options loaded for ANDROID " + gameType);
        }
        return products;
    }
}
