package com.yazino.web.payment.googlecheckout;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PaymentOptionBuilder;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.service.SafeBuyChipsPromotionService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.web.payment.googlecheckout.AndroidBuyChipStoreConfig.ChipBundleKeys;
import static com.yazino.web.payment.googlecheckout.AndroidBuyChipStoreConfig.ChipBundleKeys.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AndroidPromotionServiceTest {

    private static final Platform PLATFORM = Platform.ANDROID;
    private static final String GAME_TYPE = "Poker";
    private final BigDecimal PLAYER_ID = new BigDecimal(-10);
    private final Long PROMO_ID = 23L;
    private AndroidPromotionService underTest;

    @Mock
    SafeBuyChipsPromotionService buyChipsPromotionService;

    @Mock
    ChipBundleResolver chipBundleResolver;

    private Map<Currency, List<PaymentOption>> defaultPromotions;
    private List<Map<ChipBundleKeys, Object>> chipBundleList;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new AndroidPromotionService(buyChipsPromotionService, chipBundleResolver);
        defaultPromotions = new HashMap<Currency, List<PaymentOption>>();
        chipBundleList = new ArrayList<Map<ChipBundleKeys, Object>>();
    }

    @Test(expected = NullPointerException.class)
    public void getBuyChipsStoreConfigShouldThrowErrorIfNullBuyChipsPromotionService() {
        new AndroidPromotionService(null, chipBundleResolver);
    }

    @Test
    public void getBuyChipsStoreConfigShouldFetchPromotionDetailsFromBuyChipsPromotionService() {
        underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);
        verify(buyChipsPromotionService).getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM);
    }

    @Test
    public void getBuyChipsStoreConfigShouldCreateAndroidChipStoreConfigWithDefaultPaymentOptions() {
        defaultPromotions.put(Currency.USD, setupDefaultPaymentOptions());
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);

        List<Map<ChipBundleKeys, Object>> chipBundleList = setupDefaultGoogleCheckoutBuyChipProducts();

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = new AndroidBuyChipStoreConfig();
        expectedAndroidBuyChipStoreConfig.setChipBundleList(chipBundleList);

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig =
                underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);
        assertEquals(expectedAndroidBuyChipStoreConfig.getChipBundleList(), actualAndroidBuyChipStoreConfig.getChipBundleList());
    }

    @Test
    @Ignore("until the client can handle mixed promoted and unpromoted packages")
    public void getBuyChipsStoreConfigShouldAddPromoIdWhenAPromotionIsAppliedToAnyPackage() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        // add a promotion to the IOS_USD3 package
        paymentOptions.get(0).addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, null));
        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);
        setupDefaultGoogleCheckoutBuyChipProducts();

        chipBundleList.add(createChipBundle(new BigDecimal(5000), "IOS_USD3", null));

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = setupExpectedBuyChipStoreConfig(PROMO_ID, chipBundleList);

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig = underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);
        assertEquals(expectedAndroidBuyChipStoreConfig.getPromoId(), actualAndroidBuyChipStoreConfig.getPromoId());
    }

    @Test
    // remove when client can handle mixture of promoted and default packages
    public void getBuyChipsStoreConfigShouldNotAddPromoIdUnlessPromotionIsAppliedToAllPackages() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        // add a promotion to the IOS_USD3 package. NOTE that IOS_USD8 has NO promotion
        paymentOptions.get(0).addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, null));
        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);
        setupDefaultGoogleCheckoutBuyChipProducts();

        chipBundleList.add(createChipBundle(new BigDecimal(5000), "IOS_USD3", null));

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig = underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);
        assertNull(actualAndroidBuyChipStoreConfig.getPromoId());
    }

    @Test
    public void getBuyChipsStoreConfigShouldAddPromoChipValueWhenAPromotionIsAppliedToAllPackages() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        // add a promotion to IOS_USD3 and IOS_USD8
        setupPromotionAndChipBundleForPaymentOption(paymentOptions.get(0), new BigDecimal(1234578));
        setupPromotionAndChipBundleForPaymentOption(paymentOptions.get(1), new BigDecimal(8796543));
        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        setupDefaultGoogleCheckoutBuyChipProducts();

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = setupExpectedBuyChipStoreConfig(PROMO_ID, chipBundleList);

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig =
                underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertThat(actualAndroidBuyChipStoreConfig.getPromoId(), is(PROMO_ID));
        assertEquals(new BigDecimal(1234578),
                actualAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(PROMOTION_CHIPS));
        assertEquals(new BigDecimal(8796543),
                actualAndroidBuyChipStoreConfig.getChipBundleList().get(1).get(PROMOTION_CHIPS));
    }

    private void setupPromotionAndChipBundleForPaymentOption(PaymentOption paymentOption, BigDecimal promoChips) {
        paymentOption.addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promoChips));
//        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);

        final BigDecimal defaultChipValue = paymentOption.getNumChipsPerPurchase();
        chipBundleList.add(createChipBundle(defaultChipValue, paymentOption.getId(), promoChips));
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE,
                defaultChipValue + "-" + promoChips))
                .thenReturn(new ChipBundle(paymentOption.getId(), promoChips, defaultChipValue,
                        BigDecimal.TEN, java.util.Currency.getInstance("USD")));
    }

    @Test
    @Ignore("until the client can handle mixed promoted and unpromoted packages")
    public void getBuyChipsStoreConfigShouldAddPromoChipValueWhenAPromotionIsAppliedToAPackage() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        final BigDecimal promotionChipsPerPurchase = new BigDecimal(1234578);
        // add a promotion to the IOS_USD3
        final PaymentOption optionWithPromotedChips = paymentOptions.get(0);
        optionWithPromotedChips.addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promotionChipsPerPurchase));
        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);

        final BigDecimal defaultChipValue = optionWithPromotedChips.getNumChipsPerPurchase();
        chipBundleList.add(createChipBundle(defaultChipValue, optionWithPromotedChips.getId(), promotionChipsPerPurchase));
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE,
                defaultChipValue + "-" + promotionChipsPerPurchase))
                .thenReturn(new ChipBundle(optionWithPromotedChips.getId(), promotionChipsPerPurchase, defaultChipValue,
                        BigDecimal.TEN, java.util.Currency.getInstance("USD")));
        setupDefaultGoogleCheckoutBuyChipProducts();

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = setupExpectedBuyChipStoreConfig(PROMO_ID, chipBundleList);

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig =
                underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertEquals(expectedAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(PROMOTION_CHIPS),
                actualAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(PROMOTION_CHIPS));
    }

    @Test
    // remove when client can handle mixture of promoted and default packages
    public void getBuyChipsStoreConfigShouldNotAddPromoChipValueUnlessAllPackagesArePromoted() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        final BigDecimal promotionChipsPerPurchase = new BigDecimal(1234578);
        // add a promotion to the IOS_USD3, no promotion for IOS_USD8
        final PaymentOption optionWithPromotedChips = paymentOptions.get(0);
        optionWithPromotedChips.addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promotionChipsPerPurchase));
        defaultPromotions.put(Currency.USD, (List<PaymentOption>) paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);

        final BigDecimal defaultChipValue = optionWithPromotedChips.getNumChipsPerPurchase();
        chipBundleList.add(createChipBundle(defaultChipValue, optionWithPromotedChips.getId(), promotionChipsPerPurchase));
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE,
                defaultChipValue + "-" + promotionChipsPerPurchase))
                .thenReturn(new ChipBundle(optionWithPromotedChips.getId(), promotionChipsPerPurchase, defaultChipValue,
                        BigDecimal.TEN, java.util.Currency.getInstance("USD")));
        setupDefaultGoogleCheckoutBuyChipProducts();

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig =
                underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertNull(actualAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(PROMOTION_CHIPS));
    }

    @Test
    public void getBuyChipsStoreConfigShouldNotAddNullChipBundleWhenBundleIsNotFound() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        // the IOS_USD3 package has no matching chip bundle for promoted chip value 10000
        final BigDecimal promotionChipsPerPurchaseFor5000Package = new BigDecimal(10000);
        paymentOptions.get(0).addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promotionChipsPerPurchaseFor5000Package));
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE, "5000-10000")).thenReturn(null);

        // this promoted bundle is known
        final BigDecimal promotionChipsPerPurchaseFor15000Package = new BigDecimal(30000);
        final BigDecimal defaultChipValue = new BigDecimal(15000);
        when(chipBundleResolver.findChipBundleFor(GAME_TYPE, "15000-30000"))
                .thenReturn(new ChipBundle("IOS_USD8", promotionChipsPerPurchaseFor15000Package, defaultChipValue,
                        BigDecimal.valueOf(8L), java.util.Currency.getInstance("USD")));
        paymentOptions.get(1).addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promotionChipsPerPurchaseFor15000Package));
        defaultPromotions.put(Currency.USD, paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);

        setupDefaultGoogleCheckoutBuyChipProducts();

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = setupExpectedBuyChipStoreConfig(PROMO_ID, chipBundleList);

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig =
                underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertNull(actualAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(ChipBundleKeys.PROMOTION_CHIPS));
        assertThat((BigDecimal) actualAndroidBuyChipStoreConfig.getChipBundleList().get(1).get(ChipBundleKeys.PROMOTION_CHIPS), is(promotionChipsPerPurchaseFor15000Package));
    }

    @Test
    @Ignore("until the client can handle mixed promoted and unpromoted packages")
    public void getBuyChipsStoreConfigShouldOverrideGoogleProductIdWhenAPromotionIsApplied() {
        final List<PaymentOption> paymentOptions = setupDefaultPaymentOptions();
        final BigDecimal defaultChipValue = new BigDecimal(5000);
        final BigDecimal promotionChipValue = new BigDecimal(1234578);
        // add a promotion to package IOS_USD3
        paymentOptions.get(0).addPromotionPaymentOption(createPaymentPromotion(PROMO_ID, promotionChipValue));
        defaultPromotions.put(Currency.USD, paymentOptions);
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(defaultPromotions);
        final String google_checkout_id = "PROMOTION_ID";
        chipBundleList.add(createChipBundle(defaultChipValue, google_checkout_id, promotionChipValue));
        ChipBundle chipBundle = new ChipBundle(google_checkout_id, promotionChipValue, defaultChipValue, BigDecimal.TEN,
                java.util.Currency.getInstance("USD"));

        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = setupExpectedBuyChipStoreConfig(null, chipBundleList);

        when(chipBundleResolver.findChipBundleFor(GAME_TYPE, defaultChipValue + "-" + promotionChipValue)).thenReturn(chipBundle);
        setupDefaultGoogleCheckoutBuyChipProducts();

        final AndroidBuyChipStoreConfig actualAndroidBuyChipStoreConfig = underTest.getBuyChipStoreConfig(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertEquals(expectedAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(GOOGLE_PRODUCT_ID),
                actualAndroidBuyChipStoreConfig.getChipBundleList().get(0).get(GOOGLE_PRODUCT_ID));
    }

    private AndroidBuyChipStoreConfig setupExpectedBuyChipStoreConfig(final Long promoId, final List<Map<ChipBundleKeys, Object>> chipBundleList1) {
        AndroidBuyChipStoreConfig expectedAndroidBuyChipStoreConfig = new AndroidBuyChipStoreConfig();
        expectedAndroidBuyChipStoreConfig.setChipBundleList(chipBundleList1);
        expectedAndroidBuyChipStoreConfig.setPromoId(promoId);
        return expectedAndroidBuyChipStoreConfig;
    }


    private PromotionPaymentOption createPaymentPromotion(final Long promoId, final BigDecimal promotionChipsPerPurchase) {
        return new PromotionPaymentOption(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT,
                promoId,
                promotionChipsPerPurchase,
                null,
                null);
    }

    private Map<ChipBundleKeys, Object> createChipBundle(BigDecimal defaultChipValue,
                                                         String googlePackageId,
                                                         BigDecimal promotionChips) {
        Map<ChipBundleKeys, Object> chipBundle = new HashMap<ChipBundleKeys, Object>();
        chipBundle.put(DEFAULT_VALUE, defaultChipValue);
        chipBundle.put(GOOGLE_PRODUCT_ID, googlePackageId);
        if (promotionChips != null) {
            chipBundle.put(PROMOTION_CHIPS, promotionChips);
        }
        return chipBundle;
    }

    private List<Map<ChipBundleKeys, Object>> setupDefaultGoogleCheckoutBuyChipProducts() {
        List<BigDecimal> defaultChipValues = Arrays.asList(new BigDecimal(5000), new BigDecimal(15000));

        List<String> defaultGoogleProductIds = Arrays.asList("default1", "default2");

        List<Map<ChipBundleKeys, Object>> chipBundleList = new ArrayList<Map<ChipBundleKeys, Object>>();

        for (int i = 0; i < defaultGoogleProductIds.size(); i++) {
            chipBundleList.add(createChipBundle(defaultChipValues.get(i), defaultGoogleProductIds.get(i), null));
            when(chipBundleResolver.findChipBundleFor(GAME_TYPE, defaultChipValues.get(i).toString()))
                    .thenReturn(new ChipBundle(defaultGoogleProductIds.get(i),
                            defaultChipValues.get(i),
                            defaultChipValues.get(i),
                            defaultChipValues.get(i),
                            java.util.Currency.getInstance("USD")));
        }
        return chipBundleList;
    }

    private List<PaymentOption> setupDefaultPaymentOptions() {
        List<PaymentOption> defaultOptions = new ArrayList<PaymentOption>();
        defaultOptions.add(new PaymentOptionBuilder()
                .setId("IOS_USD3")
                .setAmountRealMoneyPerPurchase(BigDecimal.valueOf(3))
                .setNumChipsPerPurchase(BigDecimal.valueOf(5000))
                .setRealMoneyCurrency("USD")
                .setCurrencyLabel("$")
                .createPaymentOption());
        defaultOptions.add(new PaymentOptionBuilder()
                .setId("IOS_USD8")
                .setAmountRealMoneyPerPurchase(BigDecimal.valueOf(8))
                .setNumChipsPerPurchase(BigDecimal.valueOf(15000))
                .setRealMoneyCurrency("USD")
                .setCurrencyLabel("$")
                .createPaymentOption());
        return defaultOptions;
    }
}
