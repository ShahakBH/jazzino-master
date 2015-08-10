package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.chipbundle.ChipBundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.AMAZON;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class AndroidProductFactoryTest {
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "the product id";
    public static final String PROMO_PRODUCT_ID = "the promo product id";
    public static final BigDecimal DEFAULT_CHIPS = BigDecimal.valueOf(10000);
    public static final Currency USD = Currency.getInstance("USD");
    public static final BigDecimal PRICE = BigDecimal.valueOf(5);
    public static final BigDecimal PROMO_CHIPS = BigDecimal.valueOf(20000);
    public static final long PROMO_ID = 23L;

    @Mock
    private ChipBundleResolver chipBundleResolver;

    private AndroidProductFactory underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new AndroidProductFactory(chipBundleResolver);
    }

    @Test
    public void shouldReturnNullIfDefaultChipBundleNotFound() {
        PaymentOption paymentOption = createOptionWithDefaultChips();
        AndroidStoreProduct actual = underTest.getProductFor(GAME_TYPE, paymentOption, GOOGLE_CHECKOUT);

        assertThat(actual, nullValue());
    }

    @Test
    public void shouldReturnProductWithNoPromotionDetails() {
        whenDefaultChipBundleIsReturned();

        PaymentOption paymentOption = createOptionWithDefaultChips();
        AndroidStoreProduct actual = underTest.getProductFor(GAME_TYPE, paymentOption, GOOGLE_CHECKOUT);

        assertDefaultProduct(actual);
    }

    @Test
    public void shouldReturnProductWithPromotionDetails() {
        whenPromotionChipBundleIsReturned();

        PaymentOption paymentOption = createOptionWithPromotion(GOOGLE_CHECKOUT);
        AndroidStoreProduct actual = underTest.getProductFor(GAME_TYPE, paymentOption, GOOGLE_CHECKOUT);

        assertPromotionProduct(actual);
    }

    @Test
    public void shouldReturnAmazonProductWithPromotionDetails() {
        whenPromotionChipBundleIsReturned();

        PaymentOption paymentOption = createOptionWithPromotion(AMAZON);
        AndroidStoreProduct actual = underTest.getProductFor(GAME_TYPE, paymentOption, AMAZON);

        assertPromotionProduct(actual);
    }

    @Test
    public void shouldReturnProductWithoutPromotionDetailsWhenPromoChipsEqualsDefaultChips() {
        whenDefaultChipBundleIsReturned();

        PaymentOption paymentOption = createOptionWithPromotionChipsEqualToDefaultChips(GOOGLE_CHECKOUT);
        AndroidStoreProduct actual = underTest.getProductFor(GAME_TYPE, paymentOption, GOOGLE_CHECKOUT);

        assertDefaultProduct(actual);
    }

    private void assertDefaultProduct(AndroidStoreProduct product) {
        AndroidStoreProduct expected = new AndroidStoreProduct(PRODUCT_ID, DEFAULT_CHIPS);
        assertThat(product, is(expected));
    }

    private void assertPromotionProduct(AndroidStoreProduct product) {
        AndroidStoreProduct expected = new AndroidStoreProduct(PROMO_PRODUCT_ID, DEFAULT_CHIPS);
        expected.setPromoChips(PROMO_CHIPS);
        assertThat(product, is(expected));
    }

    private PaymentOption createOptionWithDefaultChips() {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setNumChipsPerPurchase(DEFAULT_CHIPS);
        return paymentOption;
    }

    private PaymentOption createOptionWithPromotion(PaymentPreferences.PaymentMethod paymentMethod) {
        PaymentOption paymentOption = createOptionWithDefaultChips();
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod, PROMO_ID, PROMO_CHIPS, "rollover", "rollover text");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        return paymentOption;
    }

    private PaymentOption createOptionWithPromotionChipsEqualToDefaultChips(PaymentPreferences.PaymentMethod paymentMethod) {
        PaymentOption paymentOption = createOptionWithDefaultChips();
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod, PROMO_ID, DEFAULT_CHIPS, "rollover", "rollover text");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        return paymentOption;
    }

    private void whenPromotionChipBundleIsReturned() {
        whenDefaultChipBundleIsReturned();
        ChipBundle bundle = new ChipBundle(PROMO_PRODUCT_ID, PROMO_CHIPS, DEFAULT_CHIPS, PRICE, USD);
        String chipBundleKey = DEFAULT_CHIPS + "-" + PROMO_CHIPS;
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE, chipBundleKey)).thenReturn(bundle);
    }

    private void whenDefaultChipBundleIsReturned() {
        ChipBundle bundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, USD);
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE, DEFAULT_CHIPS.toString())).thenReturn(bundle);
    }
}
