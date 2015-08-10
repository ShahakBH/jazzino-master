package com.yazino.web.payment.facebook;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.FACEBOOK;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

// tested for jrae needs a bit of love
public class FacebookPaymentOptionHelperTest {
    public static final String ANY_GAME = "ANY GAME";
    private FacebookPaymentOptionHelper underTest = new FacebookPaymentOptionHelper();

    @Test
    public void shouldModifyPaymentOptionIdsForAllCurrenciesIfOptionIdStartsWithOption() {
        Map<Currency, List<PaymentOption>> paymentOptionsMap = paymentOptions();
        List<PaymentOption> gbpOptions = paymentOptionsMap.get(Currency.GBP);
        assertThat(gbpOptions.get(0).getId(), is("optionGBP1"));
        assertThat(gbpOptions.get(1).getId(), is("optionGBP2"));
        assertThat(gbpOptions.get(2).getId(), is("optionGBP3"));
        List<PaymentOption> usdOptions = paymentOptionsMap.get(Currency.USD);
        assertThat(usdOptions.get(0).getId(), is("optionUSD1"));
        assertThat(usdOptions.get(1).getId(), is("will-not-modify"));

        underTest.modifyPaymentOptionIdsIn(ANY_GAME, paymentOptionsMap);

        assertThat(usdOptions.get(0).getId(), is("usd1_10_buys_0.1k"));
        assertThat(usdOptions.get(1).getId(), is("will-not-modify"));
        assertThat(gbpOptions.get(0).getId(), is("gbp1_3_buys_10k"));
        assertThat(gbpOptions.get(1).getId(), is("gbp2_5_buys_21k_78"));
        assertThat(gbpOptions.get(2).getId(), is("gbp3_10_buys_100k_78"));
    }

    @Test
    public void shouldNotTransformPaymentOptionId() {
        String transformedId = underTest.transformPaymentOptionId(ANY_GAME,
                                                                  paymentOption("will-not-modify", valueOf(4444), "USD", valueOf(444), "$", "label 2"),
                                                                  FACEBOOK);

        assertThat(transformedId, is("will-not-modify"));
    }

    @Test
    public void shouldTransformPaymentOptionIdWithoutPromotion() {
        String transformedId = underTest.transformPaymentOptionId(ANY_GAME,
                                                                  paymentOption("optionGBP1", valueOf(10000), "GBP", new BigDecimal(3.00), "£", "label 1"),
                                                                  FACEBOOK);

        assertThat(transformedId, is("gbp1_3_buys_10k"));
    }

    @Test
    public void shouldTransformPaymentOptionIdPromotionWithPromotionUsingDefaultChips() {
        // note default chips == promo chips
        PaymentOption paymentOption = paymentOptionWithPromotion("optionGBP2",
                                                                 valueOf(21000),
                                                                 "GBP",
                                                                 new BigDecimal(5.00),
                                                                 "£",
                                                                 "label 2",
                                                                 FACEBOOK,
                                                                 78L,
                                                                 valueOf(21000));
        String transformedId = underTest.transformPaymentOptionId(ANY_GAME, paymentOption, FACEBOOK);

        assertThat(transformedId, is("gbp2_5_buys_21k_78"));
    }

    @Test
    public void shouldTransformPaymentOptionIdPromotionWithPromotionUsingPromoChips() {
        // note default chips != promo chips
        PaymentOption paymentOption = paymentOptionWithPromotion("optionGBP3",
                                                                 valueOf(50000),
                                                                 "GBP",
                                                                 new BigDecimal(10.00),
                                                                 "£",
                                                                 "label 3",
                                                                 FACEBOOK,
                                                                 78L,
                                                                 valueOf(100000));
        String transformedId = underTest.transformPaymentOptionId(ANY_GAME, paymentOption, FACEBOOK);

        assertThat(transformedId, is("gbp3_10_buys_100k_78"));
    }

    private Map<Currency, List<PaymentOption>> paymentOptions() {
        Map<Currency, List<PaymentOption>> expectedPaymentOptions = newHashMap();
        expectedPaymentOptions.put(Currency.USD, Arrays.asList(
                paymentOption("optionUSD1", valueOf(100), "USD", valueOf(10), "$", "label 1"),
                paymentOption("will-not-modify", valueOf(4444), "USD", valueOf(444), "$", "label 2")));
        expectedPaymentOptions.put(Currency.GBP, Arrays.asList(
                paymentOption("optionGBP1", valueOf(10000), "GBP", new BigDecimal(3.00), "£", "label 1"),
                paymentOptionWithPromotion("optionGBP2", valueOf(21000), "GBP", new BigDecimal(5.00), "£", "label 2", FACEBOOK, 78L, valueOf(21000)),
                paymentOptionWithPromotion("optionGBP3", valueOf(50000), "GBP", new BigDecimal(10.00), "£", "label 3", FACEBOOK, 78L, valueOf(100000))));
        return expectedPaymentOptions;
    }

    private PaymentOption paymentOption(String optionId,
                                        BigDecimal chipsPerPurchase,
                                        String currencyCode,
                                        BigDecimal amountOfMoney,
                                        String currencyLabel,
                                        String upsellTitle) {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId(optionId);
        paymentOption.setCurrencyCode(currencyCode);
        paymentOption.setAmountRealMoneyPerPurchase(amountOfMoney);
        paymentOption.setNumChipsPerPurchase(chipsPerPurchase);
        paymentOption.setCurrencyLabel(currencyLabel);
        paymentOption.setUpsellTitle(upsellTitle);
        return paymentOption;
    }

    private PaymentOption paymentOptionWithPromotion(String optionId,
                                                     BigDecimal chipsPerPurchase,
                                                     String currencyCode,
                                                     BigDecimal amountOfMoney,
                                                     String currencyLabel,
                                                     String upsellTitle,
                                                     PaymentPreferences.PaymentMethod paymentMethod,
                                                     Long promoId,
                                                     BigDecimal promoChipsPerPurchase) {
        PaymentOption paymentOption = paymentOption(optionId, chipsPerPurchase, currencyCode, amountOfMoney, currencyLabel, upsellTitle);
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod, promoId,
                                                                                   promoChipsPerPurchase, "", "");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        return paymentOption;
    }

}
