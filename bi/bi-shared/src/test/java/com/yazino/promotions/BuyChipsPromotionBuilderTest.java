package com.yazino.promotions;

import com.yazino.platform.community.PaymentPreferences;
import org.junit.Test;
import strata.server.lobby.api.promotion.BuyChipsPromotion;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuyChipsPromotionBuilderTest {

    @Test
    public void withPaymentMethodsShouldReturnStringInCorrectFormat() {
        BuyChipsPromotionBuilder buyChipsPromotionBuilder = new BuyChipsPromotionBuilder();

        buyChipsPromotionBuilder.withPaymentMethods(asList(PaymentPreferences.PaymentMethod.FACEBOOK, PaymentPreferences.PaymentMethod.ITUNES));
        final BuyChipsPromotion buyChipsPromotion = buyChipsPromotionBuilder.build();

        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue(BuyChipsPromotion.PAYMENT_METHODS_KEY), is("FACEBOOK,ITUNES"));
    }

    @Test
    public void withPaymentMethodsShouldReturnNullIfPaymentMethodIsAnEmptyList() {
        BuyChipsPromotionBuilder buyChipsPromotionBuilder = new BuyChipsPromotionBuilder();

        buyChipsPromotionBuilder.withPaymentMethods(new ArrayList<PaymentPreferences.PaymentMethod>());
        final BuyChipsPromotion buyChipsPromotion = buyChipsPromotionBuilder.build();

        String expectedPaymentsValue = null;
        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue(BuyChipsPromotion.PAYMENT_METHODS_KEY), is(expectedPaymentsValue));
    }
}
