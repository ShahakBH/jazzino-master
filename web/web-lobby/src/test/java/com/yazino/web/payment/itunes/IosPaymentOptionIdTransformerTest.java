package com.yazino.web.payment.itunes;

import com.google.common.collect.ImmutableMap;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import org.junit.Test;

import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class IosPaymentOptionIdTransformerTest {
    private IosPaymentOptionIdTransformer underTest = new IosPaymentOptionIdTransformer();

    @Test
    public void transformShouldConvertNonPromoToPreferredID() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setAmountRealMoneyPerPurchase(valueOf(3));
        paymentOption.setCurrencyCode("USD");
        paymentOption.setNumChipsPerPurchase(valueOf(5000));
        paymentOption.setId("IOS_USD3");

        assertThat(underTest.transformPaymentOptionId("SLOTS", paymentOption, PaymentPreferences.PaymentMethod.ITUNES),
                is(equalTo("USD3_BUYS_5K")));
    }

    @Test
    public void transformShouldConvertNonPromoBlackjackToPreferredID() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setAmountRealMoneyPerPurchase(valueOf(3));
        paymentOption.setCurrencyCode("USD");
        paymentOption.setNumChipsPerPurchase(valueOf(5000));
        paymentOption.setId("IOS_USD3");

        assertThat(underTest.transformPaymentOptionId("BLACKJACK", paymentOption, PaymentPreferences.PaymentMethod.ITUNES),
                is(equalTo("BJ_USD3_BUYS_5K")));
    }

    @Test
    public void transformShouldConvertPromoToPreferredID() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setAmountRealMoneyPerPurchase(valueOf(3));
        paymentOption.setCurrencyCode("USD");
        paymentOption.setNumChipsPerPurchase(valueOf(5000));
        final PromotionPaymentOption option = new PromotionPaymentOption(PaymentPreferences.PaymentMethod.ITUNES, 123L, valueOf(10000), "rolly", "rolllerr");
        final Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> promos = ImmutableMap.of(PaymentPreferences.PaymentMethod.ITUNES, option);
        paymentOption.setPromotions(promos);
        paymentOption.setId("IOS_USD3");

        assertThat(underTest.transformPaymentOptionId("SLOTS", paymentOption, PaymentPreferences.PaymentMethod.ITUNES),
                is(equalTo("USD3_BUYS_10K_P100")));
    }

    @Test
    public void transformShouldConvertBlackjackPromoToPreferredID() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setAmountRealMoneyPerPurchase(valueOf(3));
        paymentOption.setCurrencyCode("USD");
        paymentOption.setNumChipsPerPurchase(valueOf(5000));
        final PromotionPaymentOption option = new PromotionPaymentOption(PaymentPreferences.PaymentMethod.ITUNES, 123L, valueOf(10000), "rolly", "rolllerr");
        final Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> promos = ImmutableMap.of(PaymentPreferences.PaymentMethod.ITUNES, option);
        paymentOption.setPromotions(promos);
        paymentOption.setId("IOS_USD3");

        assertThat(underTest.transformPaymentOptionId("BLACKJACK", paymentOption, PaymentPreferences.PaymentMethod.ITUNES),
                is(equalTo("BJ_USD3_BUYS_10K_P100")));
    }


}
