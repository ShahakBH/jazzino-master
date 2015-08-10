package com.yazino.web.payment.googlecheckout;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.web.payment.googlecheckout.AndroidBuyChipStoreConfig.ChipBundleKeys;
import static com.yazino.web.payment.googlecheckout.AndroidBuyChipStoreConfig.ChipBundleKeys.*;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Returns current chip package details for player.
 *
 * @see AndroidBuyChipStoreConfig
 * @deprecated use AndroidPromotionServiceV3 instead, when all game clients use /api/1.0/payments/android/google/products or higher
 */
@Service
public class AndroidPromotionService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidPromotionService.class);

    private final BuyChipsPromotionService buyChipsPromotionService;
    private final ChipBundleResolver chipBundleResolver;

    @Autowired
    public AndroidPromotionService(final BuyChipsPromotionService buyChipsPromotionService,
                                   final ChipBundleResolver chipBundleResolver) {
        notNull(buyChipsPromotionService);
        notNull(chipBundleResolver);
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.chipBundleResolver = chipBundleResolver;
    }

    public AndroidBuyChipStoreConfig getBuyChipStoreConfig(final BigDecimal playerId,
                                                           final Platform platform,
                                                           final String gameType) {
        notNull(gameType);
        final Map<Currency, List<PaymentOption>> buyChipsPaymentOptionsForAndroid =
                buyChipsPromotionService.getBuyChipsPaymentOptionsFor(playerId, platform);
        final AndroidBuyChipStoreConfig androidBuyChipStoreConfig = new AndroidBuyChipStoreConfig();

        if (buyChipsPaymentOptionsForAndroid.containsKey(Currency.USD)) {
            final List<PaymentOption> paymentOptionList = buyChipsPaymentOptionsForAndroid.get(Currency.USD);

            // TODO sort out this bag of shite
            // Validate payment options, currently if any option has a promoted value, then ALL options must have
            // promoted values. If we mix promoted packages with non-promoted packages, the client will be fubar. It
            // will crash when attempting to display the list of packages: the client will throw a NPE as below
            /*
            I/air.com.yazino.android.slots(21562): TypeError: Error #1009: Cannot access a property or method of a null object reference.
            I/air.com.yazino.android.slots(21562): 	at senet.client.flexgoodies.utils::StringUtils$/insertCommaToThousands()[/Users/rsandys/Projects/games-flex/client-flex4/flex4-corelib/src/senet/client/flexgoodies/utils/StringUtils.as:19]
            I/air.com.yazino.android.slots(21562): 	at components::BuyChipsSkinnableComponent/set listDataProvider()[/Users/rsandys/Projects/games-flex/client-flex4/flex4-wheeldeal-android/flex4-wheeldeal-android-large/target/build/shell/src/components/BuyChipsSkinnableComponent.as:156]
             */
            // a workaround would be to add the default value as the promoted value BUT this would lead to the client
            // displaying default packages as promoted packages. This is also undesirable.
            // The TEMP solution is to ignore promotions with missing overrides (i.e. just return default values with
            // null promo id). This is also required is the player is in the control group.
            // Mel has agreed to only create android promotions with ALL packages overridden and with a control
            // group % of 0, until client side and server side fixes.

            Long promoId = null;
            boolean ignorePromotion = false;
            int paymentOptionsWithPromotionCount = 0;
            for (PaymentOption paymentOption : paymentOptionList) {
                if (paymentOption.hasPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT)) {
                    ++paymentOptionsWithPromotionCount;
                    final PromotionPaymentOption promotionOption = paymentOption.getPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT);
                    promoId = promotionOption.getPromoId();
                    final BigDecimal promoChips = promotionOption.getPromotionChipsPerPurchase();
                    if (promoChips == null || promoChips.compareTo(paymentOption.getNumChipsPerPurchase()) <= 0) {
                        ignorePromotion = true;
                        break;
                    }
                }
            }
            if (paymentOptionsWithPromotionCount > 0 && paymentOptionsWithPromotionCount != paymentOptionList.size()) {
                // at least one package has a promoted value and at least one doesn't
                ignorePromotion = true;
            }

            if (ignorePromotion) {
                // another package has promotion and this one doesn't or player is in control group
                LOG.error("An ANDROID buy chip promotion [promoId={}] has at least one package that is NOT "
                        + "overridden; or the control group percentage is not zero. Speak to Mel who will "
                        + "update the promotion accordingly",
                        promoId);
            }

            for (PaymentOption paymentOption : paymentOptionList) {
                final Map<ChipBundleKeys, Object> buyChipBundle = new HashMap<ChipBundleKeys, Object>();

                buyChipBundle.put(DEFAULT_VALUE, paymentOption.getNumChipsPerPurchase());
                final ChipBundle defaultChipBundle = chipBundleResolver.findChipBundleFor(
                        gameType,
                        paymentOption.getNumChipsPerPurchase().toPlainString());
                buyChipBundle.put(GOOGLE_PRODUCT_ID, defaultChipBundle.getProductId());

                if (!ignorePromotion && paymentOption.hasPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT)) {
                    final PromotionPaymentOption promotion =
                            paymentOption.getPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT);
                    LOG.debug("Payment option has promotion: {}", promotion);

                    final String chipValueId = buildChipValueId(paymentOption, promotion);
                    final ChipBundle chipBundle = chipBundleResolver.findChipBundleFor(gameType, chipValueId);
                    // The chip bundle may be null if the package hasn't been overridden in the promotion or if the
                    // player is in the control group.
                    // This whole promotion stuff is bit of a mess and should be reconsidered.
                    if (chipBundle == null) {
                        LOG.debug("Ignoring promotion[{}], chipValueId[{}] since no matching bundle found",
                                promotion.getPromoId(), chipValueId);
                    } else {
                        androidBuyChipStoreConfig.setPromoId(promotion.getPromoId());
                        buyChipBundle.put(PROMOTION_CHIPS, promotion.getPromotionChipsPerPurchase());
                        buyChipBundle.put(GOOGLE_PRODUCT_ID, chipBundle.getProductId());
                    }
                }
                androidBuyChipStoreConfig.addToChipBundleList(buyChipBundle);
            }
        }

        return androidBuyChipStoreConfig;
    }

    private String buildChipValueId(final PaymentOption paymentOption, final PromotionPaymentOption promotion) {
        if (promotion.getPromotionChipsPerPurchase() == null) {
            return paymentOption.getNumChipsPerPurchase().toPlainString();
        } else {
            return paymentOption.getNumChipsPerPurchase().toPlainString()
                    + "-"
                    + promotion.getPromotionChipsPerPurchase().toPlainString();
        }
    }
}
