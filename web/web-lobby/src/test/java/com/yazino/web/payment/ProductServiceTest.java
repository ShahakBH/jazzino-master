package com.yazino.web.payment;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import com.yazino.web.api.payments.BestValueProductPolicy;
import com.yazino.web.api.payments.ChipProduct;
import com.yazino.web.api.payments.ChipProducts;
import com.yazino.web.api.payments.MostPopularProductPolicy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

public class ProductServiceTest {
    private static final Platform PLATFORM = Platform.ANDROID;
    private static final PaymentPreferences.PaymentMethod PAYMENT_METHOD = PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "SLOTS";
    private static final Long PROMO_ID = 9876L;
    public static final String USD_CODE = Currency.USD.getCode();
    public static final String GBP_CODE = Currency.GBP.getCode();
    public static final String USD_CURRENCY_LABEL = "$";
    public static final String GBP_CURRENCY_LABEL = "£";
    public static final BigDecimal USD_OPTION1_PRICE = BigDecimal.valueOf(3.00);
    public static final BigDecimal USD_OPTION2_PRICE = BigDecimal.valueOf(5.00);
    public static final BigDecimal USD_OPTION3_PRICE = BigDecimal.valueOf(10.00);
    public static final BigDecimal USD_OPTION3_PROMO_CHIPS = BigDecimal.valueOf(100000);
    public static final BigDecimal USD_OPTION3_CHIPS = BigDecimal.valueOf(50000);
    public static final BigDecimal USD_OPTION2_CHIPS = BigDecimal.valueOf(21000);
    public static final BigDecimal USD_OPTION1_CHIPS = BigDecimal.valueOf(10000);
    public static final String USD_OPTION1_ID = "optionUSD1";
    public static final String USD_OPTION2_ID = "optionUSD2";
    public static final String USD_OPTION3_ID = "optionUSD3";
    public static final String TRANSFORM_POSTFIX = "-transformed";

    private ProductService underTest;

    @Mock
    private HttpServletRequest request;
    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;
    @Mock
    private MostPopularProductPolicy mostPopularProductPolicy;
    @Mock
    private BestValueProductPolicy bestValueProductPolicy;
    @Mock
    private ProductPreferredCurrencyService productPreferredCurrencyService;

    // simple payment option id transformer. post fixes "-transformed" to option id
    private PaymentOptionIdTransformer paymentOptionIdTransformer = new PaymentOptionIdTransformer() {
        @Override
        public String transformPaymentOptionId(String gameType, PaymentOption paymentOption, final PaymentPreferences.PaymentMethod paymentMethod) {
            if (paymentOption.getId().startsWith("option")) {
                return paymentOption.getId() + TRANSFORM_POSTFIX;
            }
            return null;
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new ProductService(PLATFORM,
                                       PAYMENT_METHOD,
                                       buyChipsPromotionService,
                                       mostPopularProductPolicy,
                                       bestValueProductPolicy,
                                       paymentOptionIdTransformer,
                                       productPreferredCurrencyService);
        when(productPreferredCurrencyService.getPreferredCurrency(Matchers.any(ProductRequestContext.class))).thenReturn(Currency.USD);
    }

    @Test
    public void shouldReturnEmptyChipProductsWhenBuyChipsPromotionServiceReturnsNoPaymentOptions() {
        Map<Currency, List<PaymentOption>> expectedPaymentOptions = newHashMap();
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, Platform.ANDROID)).thenReturn(expectedPaymentOptions);

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts, is(new ChipProducts()));
    }

    @Test
    public void shouldReturnEmptyChipProductsWhenBuyChipsPromotionServiceReturnsNoPaymentOptionsForPreferredCurrency() {
        mockPromotionServiceWith(gbpPaymentOptions());

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts, is(new ChipProducts()));
    }

    @Test
    public void shouldReturnChipProductsInPlayersPreferredCurrency() {
        mockPromotionServiceWith(gbpPaymentOptions(), usdPaymentOptions());

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts.getChipProducts(), is(expectedUSDChipProductsChipProducts()));
    }

    @Test
    public void shouldIgnorePaymentOptionsWhenTransformerFailsToTransformOptionId() {
        mockPromotionServiceWith(usdPaymentOptions());

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts.getChipProducts(), is(expectedUSDChipProductsChipProducts()));
    }

    @Test
    public void shouldFlagMostPopularProduct() {
        mockPromotionServiceWith(gbpPaymentOptions(), usdPaymentOptions());
        when(mostPopularProductPolicy.findProductIdOfMostPopularProduct(anyList())).thenReturn(USD_OPTION2_ID);

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts.getMostPopularProductId(), is(USD_OPTION2_ID));
    }

    @Test
    public void shouldFlagBestValueProduct() {
        mockPromotionServiceWith(gbpPaymentOptions(), usdPaymentOptions());
        when(bestValueProductPolicy.findProductIdOfBestValueProduct(anyList())).thenReturn(USD_OPTION3_ID);

        ChipProducts actualProducts = underTest.getAvailableProducts(request, PLAYER_ID, GAME_TYPE);

        assertThat(actualProducts.getBestProductId(), is(USD_OPTION3_ID));
    }

    private Map<Currency, List<PaymentOption>> mockPromotionServiceWith(List<PaymentOption>... optionsForCurrency) {
        Map<Currency, List<PaymentOption>> expectedPaymentOptions = newHashMap();
        for (List<PaymentOption> options : optionsForCurrency) {
            expectedPaymentOptions.put(Currency.valueOf(options.get(0).getCurrencyCode()), options);
        }
        when(buyChipsPromotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, PLATFORM)).thenReturn(expectedPaymentOptions);
        return expectedPaymentOptions;
    }

    // this ties up with usdPaymentOptions
    private List<ChipProduct> expectedUSDChipProductsChipProducts() {
        return newArrayList(
                new ChipProduct.ProductBuilder()
                        .withProductId(USD_OPTION1_ID + TRANSFORM_POSTFIX)
                        .withPrice(USD_OPTION1_PRICE)
                        .withChips(USD_OPTION1_CHIPS)
                        .withCurrencyLabel(USD_CURRENCY_LABEL)
                        .withLabel("Starter Style")
                        .build(),
                new ChipProduct.ProductBuilder()
                        .withProductId(USD_OPTION2_ID + TRANSFORM_POSTFIX)
                        .withPrice(USD_OPTION2_PRICE)
                        .withChips(USD_OPTION2_CHIPS)
                        .withCurrencyLabel(USD_CURRENCY_LABEL)
                        .withLabel("Clever Competitor")
                        .withPromoId(PROMO_ID) // has id but no chips since payment option promo chips is same as default chips
                        .build(),
                new ChipProduct.ProductBuilder()
                        .withProductId(USD_OPTION3_ID + TRANSFORM_POSTFIX)
                        .withPrice(USD_OPTION3_PRICE)
                        .withChips(USD_OPTION3_CHIPS)
                        .withCurrencyLabel(USD_CURRENCY_LABEL)
                        .withLabel("Lucky Break")
                        .withPromoId(PROMO_ID)
                        .withPromoChips(USD_OPTION3_PROMO_CHIPS)
                        .build());
    }

    private List<PaymentOption> gbpPaymentOptions() {
        return newArrayList(paymentOption("optionGBP1", valueOf(100), GBP_CODE, valueOf(10), GBP_CURRENCY_LABEL, "label 1", 1),
                            paymentOption("optionGBP2", valueOf(10000), GBP_CODE, valueOf(100), GBP_CURRENCY_LABEL, "label 2", 2),
                            paymentOption("optionGBP3", valueOf(100000), GBP_CODE, valueOf(1000), GBP_CURRENCY_LABEL, "label 3", 3));
    }

    private List<PaymentOption> usdPaymentOptions() {
        return newArrayList(paymentOption(USD_OPTION1_ID, USD_OPTION1_CHIPS, USD_CODE, USD_OPTION1_PRICE, USD_CURRENCY_LABEL, "label 1", 1),
                            paymentOptionWithPromotion(USD_OPTION2_ID,
                                                       USD_OPTION2_CHIPS,
                                                       USD_CODE,
                                                       USD_OPTION2_PRICE,
                                                       USD_CURRENCY_LABEL,
                                                       "label 2",
                                                       PAYMENT_METHOD,
                                                       PROMO_ID,
                                                       USD_OPTION2_CHIPS,
                                                       2),
                            paymentOptionWithPromotion(USD_OPTION3_ID,
                                                       USD_OPTION3_CHIPS,
                                                       USD_CODE,
                                                       USD_OPTION3_PRICE,
                                                       USD_CURRENCY_LABEL,
                                                       "label 3",
                                                       PAYMENT_METHOD,
                                                       PROMO_ID,
                                                       USD_OPTION3_PROMO_CHIPS,
                                                       3),
                            paymentOption("will not transform", null, "", null, "", "", 0));
    }

    private PaymentOption paymentOption(String optionId,
                                        BigDecimal chipsPerPurchase,
                                        String currencyCode,
                                        BigDecimal amountOfMoney,
                                        String currencyLabel,
                                        String upsellTitle,
                                        int level) {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId(optionId);
        paymentOption.setCurrencyCode(currencyCode);
        paymentOption.setAmountRealMoneyPerPurchase(amountOfMoney);
        paymentOption.setNumChipsPerPurchase(chipsPerPurchase);
        paymentOption.setCurrencyLabel(currencyLabel);
        paymentOption.setUpsellTitle(upsellTitle);
        paymentOption.setLevel(level);
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
                                                     BigDecimal promoChipsPerPurchase,
                                                     int level) {
        PaymentOption paymentOption = paymentOption(optionId, chipsPerPurchase, currencyCode, amountOfMoney, currencyLabel, upsellTitle, level);
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod, promoId,
                                                                                   promoChipsPerPurchase, "", "");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        return paymentOption;
    }

}
