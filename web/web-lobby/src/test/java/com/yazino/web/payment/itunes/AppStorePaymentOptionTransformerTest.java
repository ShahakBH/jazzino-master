package com.yazino.web.payment.itunes;

import org.junit.Test;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * This class
 */
public class AppStorePaymentOptionTransformerTest {

    private final Map<String, String> mMappings = new HashMap<String, String>();
    private final AppStorePaymentOptionTransformer mTransformer = new AppStorePaymentOptionTransformer(mMappings);

    @Test
    public void shouldTransformNullIntoEmptyChipOptions() throws Exception {
        assertNull(mTransformer.apply(null));
    }

    @Test
    public void shouldTransformPaymentOptionWithoutPromotion() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "IOS_USD5", 10);
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldTransformSinglePaymentOptionWithoutPromotionUsingMappedID() throws Exception {
        mMappings.put("IOS_USD5", "BLACKJACK_USD5_BUYS_50K");
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "BLACKJACK_USD5_BUYS_50K", 10);
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldHaveNullHeaderWhenNoHeaders() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertNull(actual.getHeader());
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldHaveNullSubHeaderWhenNoSubHeaders() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertNull(actual.getSubHeader());
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldWriteCorrectHeaderWhenNoPromotion() throws Exception {
        mTransformer.setStandardProductHeaders(AppStoreTestUtils.toMap("IOS_USD5", "FOO"));
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertEquals("FOO", actual.getHeader());
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldWriteCorrectHeaderOntoPromotion() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 11);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertEquals("10% EXTRA", actual.getPromotion().getHeader());
    }

    @Test
    public void shouldWriteCorrectSubHeaderWhenNoPromotion() throws Exception {
        mTransformer.setStandardProductSubHeaders(AppStoreTestUtils.toMap("IOS_USD5", "BAR"));
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 0);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertEquals("BAR", actual.getSubHeader());
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldWriteCorrectSubHeaderOntoPromotion() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 11);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertEquals("11 chips", actual.getPromotion().getSubHeader());
    }

    @Test
    public void shouldWriteCorrectlyFormattedSubHeaderOntoPromotion() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10000, 20000);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertEquals("20,000 chips", actual.getPromotion().getSubHeader());
    }

    @Test
    public void shouldTransformSinglePaymentOptionWithPromotion() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 20);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "IOS_USD5", 10);
        assertChipPromotion(actual.getPromotion(), "IOS_USD5_X100", 20);
    }

    @Test
    public void shouldTransformSinglePaymentOptionWithPromotionOver100Percent() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 30);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "IOS_USD5", 10);
        assertChipPromotion(actual.getPromotion(), "IOS_USD5_X200", 30);
    }

    @Test
    public void shouldNotHavePromotionWhenPromotionChipValueIsSameAsStandard() throws Exception {
        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 10);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "IOS_USD5", 10);
        assertNull(actual.getPromotion());
    }

    @Test
    public void shouldTransformSinglePaymentOptionWithPromotionUsingMappedID() throws Exception {
        mMappings.put("IOS_USD5", "BLACKJACK_USD5_BUYS_50K");
        mMappings.put("IOS_USD5_X100", "BLACKJACK_USD5_BUYS_50K_X100");

        PaymentOption option = AppStoreTestUtils.toOption(5, 10, 20);
        AppStoreChipPackage actual = mTransformer.apply(option);
        assertChipPackage(actual, "BLACKJACK_USD5_BUYS_50K", 10);
        assertChipPromotion(actual.getPromotion(), "BLACKJACK_USD5_BUYS_50K_X100", 20);
    }

    private static void assertChipPackage(AppStoreChipPackage actual, String expectedIdentifier, int expectedChips) {
        assertEquals(expectedIdentifier, actual.getIdentifier());
        assertEquals(BigDecimal.valueOf(expectedChips), actual.getChips());
    }

    private static void assertChipPromotion(AppStoreChipPromotion actual, String expectedIdentifier, int expectedChips) {
        assertEquals(expectedIdentifier, actual.getIdentifier());
        assertEquals(BigDecimal.valueOf(expectedChips), actual.getChips());
    }

}
