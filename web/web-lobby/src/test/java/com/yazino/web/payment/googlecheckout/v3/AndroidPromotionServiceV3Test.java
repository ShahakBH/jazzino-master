package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PaymentOptionBuilder;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class AndroidPromotionServiceV3Test {

    private static final Platform PLATFORM = Platform.ANDROID;
    private static final String GAME_TYPE = "Poker";
    public static final BigDecimal PACKAGE_1_DEFAULT_CHIPS = BigDecimal.valueOf(5000);
    public static final BigDecimal PACKAGE_2_DEFAULT_CHIPS = BigDecimal.valueOf(15000);
    public static final String USD = "USD";
    public static final BigDecimal PACKAGE_2_PROMO_CHIPS = BigDecimal.valueOf(30000);
    public static final String PACKAGE_1_PAYMENT_OPTION_ID = "IOS_USD3";
    public static final String PACKAGE_2_PAYMENT_OPTION_ID = "IOS_USD8";
    public static final BigDecimal PACKAGE_1_PRICE = BigDecimal.valueOf(3);
    public static final BigDecimal PACKAGE_2_PRICE = BigDecimal.valueOf(8);
    private final BigDecimal PLAYER_ID = new BigDecimal(-10);
    private final Long PROMO_ID = 23L;
    private AndroidPromotionServiceV3 underTest;


    @Mock
    BuyChipsPromotionService buyChipsPromotionService;

    @Mock
    AndroidProductFactory androidProductFactory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new AndroidPromotionServiceV3(buyChipsPromotionService, androidProductFactory);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowErrorIfNullBuyChipsPromotionService() {
        new AndroidPromotionServiceV3(null, androidProductFactory);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowErrorIfNullAndroidProductFactory() {
        new AndroidPromotionServiceV3(buyChipsPromotionService, null);
    }

    @Test
    public void shouldCreateProductsWithoutPromotion() {
        Map<Currency, List<PaymentOption>> paymentOptions;
        paymentOptions = new HashMap<>();
        paymentOptions.put(Currency.USD, setUpPaymentOptionsWithoutPromotion());

        whenPromotionServiceReturns(paymentOptions);
        List<PaymentOption> usdPaymentOptions = paymentOptions.get(Currency.USD);
        whenProductFactoryReturnsAProductForEachPaymentOption(usdPaymentOptions);

        AndroidStoreProducts actualProducts = underTest.getAvailableProducts(PLAYER_ID, PLATFORM, GAME_TYPE);

        AndroidStoreProducts expectedProducts = createProductsFromPaymentOptions(usdPaymentOptions);
        assertThat(actualProducts.getProducts(), is(expectedProducts.getProducts()));
        assertNull(actualProducts.getPromoId());
    }

    @Test
    public void shouldCreateProductsWithPromotion() {
        Map<Currency, List<PaymentOption>> paymentOptions;
        paymentOptions = new HashMap<>();
        paymentOptions.put(Currency.USD, setUpPaymentOptionsWithPromotion());

        whenPromotionServiceReturns(paymentOptions);
        List<PaymentOption> usdPaymentOptions = paymentOptions.get(Currency.USD);
        whenProductFactoryReturnsAProductForEachPaymentOption(usdPaymentOptions);

        AndroidStoreProducts actualProducts = underTest.getAvailableProducts(PLAYER_ID, PLATFORM, GAME_TYPE);

        AndroidStoreProducts expectedProducts = createProductsFromPaymentOptions(usdPaymentOptions);
        assertThat(actualProducts.getProducts(), is(expectedProducts.getProducts()));
        assertThat(actualProducts.getPromoId(), is(PROMO_ID));
    }

    private AndroidStoreProducts createProductsFromPaymentOptions(List<PaymentOption> paymentOptions) {
        AndroidStoreProducts expectedProducts = new AndroidStoreProducts();
        expectedProducts.setProducts(createProductsForPaymentOption(paymentOptions));
        return expectedProducts;
    }

    @Test
    public void shouldReturnEmptyProductsWhenNoPaymentOptionsFound() {
        whenPromotionServiceReturns(null);

        AndroidStoreProducts actualProducts = underTest.getAvailableProducts(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertTrue(actualProducts.getProducts().isEmpty());
    }

    @Test
    public void shouldReturnEmptyProductsWhenNoUSDPaymentOptionsFound() {
        whenPromotionServiceReturns(new HashMap<Currency, List<PaymentOption>>());

        AndroidStoreProducts actualProducts = underTest.getAvailableProducts(PLAYER_ID, PLATFORM, GAME_TYPE);

        assertTrue(actualProducts.getProducts().isEmpty());
    }

    private Set<AndroidStoreProduct> createProductsForPaymentOption(List<PaymentOption> paymentOptions) {
        Set<AndroidStoreProduct> products = new HashSet<>();
        for (PaymentOption option : paymentOptions) {
            products.add(createProductForPaymentOption(option));
        }
        return products;
    }

    private void whenProductFactoryReturnsAProductForEachPaymentOption(List<PaymentOption> paymentOptions) {
        for (PaymentOption paymentOption : paymentOptions) {
            AndroidStoreProduct product = createProductForPaymentOption(paymentOption);
            when(androidProductFactory.getProductFor(GAME_TYPE, paymentOption, GOOGLE_CHECKOUT)).thenReturn(product);
        }
    }

    // creates a product with id equal to <default chips>-<price>
    private AndroidStoreProduct createProductForPaymentOption(PaymentOption option) {
        AndroidStoreProduct product = new AndroidStoreProduct(option.getNumChipsPerPurchase() + "-" + option.getAmountRealMoneyPerPurchase(), option.getNumChipsPerPurchase());
        if (option.hasPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT)) {
            PromotionPaymentOption promotion = option.getPromotion(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT);
            product.setPromoChips(promotion.getPromotionChipsPerPurchase());
        }
        return product;
    }

    private void whenPromotionServiceReturns(Map<Currency, List<PaymentOption>> paymentOptionsWithoutPromotions) {
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(paymentOptionsWithoutPromotions);
    }

    /* creates 2 options*/
    private List<PaymentOption> setUpPaymentOptionsWithoutPromotion() {
        List<PaymentOption> defaultOptions = new ArrayList<PaymentOption>();
        defaultOptions.add(new PaymentOptionBuilder()
                .setId(PACKAGE_1_PAYMENT_OPTION_ID)
                .setAmountRealMoneyPerPurchase(PACKAGE_1_PRICE)
                .setNumChipsPerPurchase(PACKAGE_1_DEFAULT_CHIPS)
                .setRealMoneyCurrency(USD)
                .setCurrencyLabel("$")
                .createPaymentOption());
        PaymentOption option2 = new PaymentOptionBuilder()
                .setId(PACKAGE_2_PAYMENT_OPTION_ID)
                .setAmountRealMoneyPerPurchase(PACKAGE_2_PRICE)
                .setNumChipsPerPurchase(PACKAGE_2_DEFAULT_CHIPS)
                .setRealMoneyCurrency(USD)
                .setCurrencyLabel("$")
                .createPaymentOption();
        //option2.addPromotionPaymentOption(createPromotionPaymentOption(PROMO_ID, PACKAGE_2_PROMO_CHIPS));
        defaultOptions.add(option2);
        return defaultOptions;
    }

    /* creates 2 options, the second has a promotion*/
    private List<PaymentOption> setUpPaymentOptionsWithPromotion() {
        List<PaymentOption> defaultOptions = setUpPaymentOptionsWithoutPromotion();
        defaultOptions.get(1).addPromotionPaymentOption(createPromotionPaymentOption(PROMO_ID, PACKAGE_2_PROMO_CHIPS));
        return defaultOptions;
    }

    private PromotionPaymentOption createPromotionPaymentOption(final Long promoId, final BigDecimal promotionChipsPerPurchase) {
        return new PromotionPaymentOption(PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT,
                promoId,
                promotionChipsPerPurchase,
                null,
                null);
    }
}
