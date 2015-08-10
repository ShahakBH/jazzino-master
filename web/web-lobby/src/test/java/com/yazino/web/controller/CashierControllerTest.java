package com.yazino.web.controller;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.web.api.payments.MostPopularProductPolicy;
import com.yazino.web.data.CurrencyRepository;
import com.yazino.web.domain.CashierConfiguration;
import com.yazino.web.domain.CashierConfigurationContainer;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import com.yazino.web.domain.facebook.FacebookUserCurrency;
import com.yazino.web.domain.payment.RegisteredCardQueryResult;
import com.yazino.web.domain.payment.RegisteredCardsQueryResultBuilder;
import com.yazino.web.payment.creditcard.CreditCardService;
import com.yazino.web.payment.creditcard.PurchaseOutcomeMapper;
import com.yazino.web.payment.creditcard.worldpay.WorldPayCreditCardQueryService;
import com.yazino.web.payment.facebook.FacebookPaymentOptionHelper;
import com.yazino.web.service.FacebookCurrencyService;
import com.yazino.web.service.FacebookService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.FACEBOOK;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.PAYPAL;
import static com.yazino.web.controller.CashierController.REGISTERED_CARDS_PARAMETER;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CashierControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final long PROMO_ID = 666l;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private CashierConfigurationContainer cashierConfigurationContainer;
    @Mock
    private PlayerService playerService;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private BuyChipsPromotionService promotionService;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;

    private FacebookPaymentOptionHelper facebookPaymentOptionHelper;
    @Mock
    private FacebookCurrencyService facebookCurrencyService;
    @Mock
    private FacebookService facebookService;
    @Mock
    private FacebookClientFactory facebookClientFactory;
    @Mock
    private WorldPayCreditCardQueryService worldPayCreditCardQueryService;
    @Mock
    private CreditCardService creditCardService;
    @Mock
    private PurchaseOutcomeMapper purchaseOutcomeMapper;

    private MostPopularProductPolicy popularProductPolicy;
    private CashierController underTest;

    @Before
    public void init() {
        popularProductPolicy = new MostPopularProductPolicy();
        facebookPaymentOptionHelper = new FacebookPaymentOptionHelper();
        underTest = new CashierController(cashierConfigurationContainer,
                playerService,
                lobbySessionCache,
                cookieHelper,
                siteConfiguration,
                promotionService,
                referenceService,
                currencyRepository,
                facebookPaymentOptionHelper,
                facebookCurrencyService,
                worldPayCreditCardQueryService,
                creditCardService,
                purchaseOutcomeMapper) {
            @Override
            protected Currency getDefaultCurrencyFromIP(final HttpServletRequest request) {
                return Currency.GBP;
            }
        };
        //this effectively mocks out the geolo.
        when(httpServletRequest.getRemoteAddr()).thenReturn("88.211.55.18");
        when(referenceService.getPreferredCurrency("GB")).thenReturn(Currency.GBP);
        when(currencyRepository.getAcceptedCurrencies()).thenReturn(
                newLinkedHashSet(asList(Currency.USD, Currency.GBP)));

        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        given(promotionService.getPaymentOptionFor(PLAYER_ID, PROMO_ID, PAYPAL, "optionId")).willReturn(paymentOption(null, PAYPAL,
                BigDecimal.ONE, BigDecimal.ONE, "GBP", null));

        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(true);
        given(cashierConfigurationContainer.getCashierConfiguration("paypal")).willReturn(cashierConfiguration());
    }

    @Test
    public void whenRequestingPaymentViewCashierConfigIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        // THEN cashier config  is added to the model
        CashierConfigurationContainer actualCashierConfigurationContainer = (CashierConfigurationContainer) modelMap.get(
                "cashierConfiguration");
        assertThat(actualCashierConfigurationContainer, is(cashierConfigurationContainer));
    }

    @Test
    public void whenRequestingPaymentViewAcceptedCurrencyMapIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        // THEN cashier config  is added to the model
        Map<String, String> expectedAcceptedCurrencies = new LinkedHashMap<String, String>(Currency.values().length);
        for (Currency acceptedCurrency : asList(Currency.USD, Currency.GBP)) {
            expectedAcceptedCurrencies.put(acceptedCurrency.getCode(), acceptedCurrency.getDescription());
        }
        Map<String, String> actualAcceptedCurrencies = (Map<String, String>) modelMap.get("acceptedCurrencies");
        assertThat(actualAcceptedCurrencies, is(expectedAcceptedCurrencies));
    }

    @Test
    public void whenRequestingPaymentViewPaymentOptionsAreAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        // AND player's preferred payment details
        given(playerService.getPaymentPreferences(PLAYER_ID)).willReturn(
                new PaymentPreferences(Currency.USD, PAYPAL, "US"));
        // AND promotion service returns these payment options
        Map<Currency, List<PaymentOption>> paymentOptionsMap = new HashMap<Currency, List<PaymentOption>>();
        paymentOptionsMap.put(Currency.USD, Arrays.asList(new PaymentOption()));
        given(promotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, Platform.WEB)).willReturn(paymentOptionsMap);

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        // THEN payment options map is added to the model
        Map<Currency, List<PaymentOption>> actualPaymentOptionsMap = (Map<Currency, List<PaymentOption>>) modelMap.get("paymentOptions");
        assertThat(actualPaymentOptionsMap, is(paymentOptionsMap));
    }

    //WEB 1630 commented out to be replaced after
    @Test
    public void whenRequestingPaymentViewIfRequestCurrencyIsNullIgnorePaymentPreferencesCurrencyAndUseGeoIpInModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        // AND player's preferred payment details
        given(playerService.getPaymentPreferences(PLAYER_ID)).willReturn(
                new PaymentPreferences(Currency.USD, PaymentPreferences.PaymentMethod.PAYPAL, "US"));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        String currency = (String) modelMap.get("preferredCurrency");
        assertThat(currency, is(Currency.GBP.getCode()));
    }

    @Test
    public void whenRequestingPaymentViewIfRequestCurrencyIsNotNullAddRequestCurrencyToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, "USD", null, null);

        String currency = (String) modelMap.get("preferredCurrency");
        assertThat(currency, is("USD"));
    }

    @Test
    public void whenRequestingPaymentViewCurrencyAddedToModelDefaultsToGeoIPLookup() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        String currency = (String) modelMap.get("preferredCurrency");
        assertThat(currency, is("GBP"));
    }

    @Test
    public void whenRequestingPaymentViewRequestPaymentTypeRatherThanPreferredPreferencesPaymentTypeIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        // AND player's preferred payment details
        given(playerService.getPaymentPreferences(PLAYER_ID)).willReturn(
                new PaymentPreferences(Currency.USD, PAYPAL, "US"));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, PaymentPreferences.PaymentMethod.TRIALPAY.name());

        PaymentPreferences.PaymentMethod preferredPaymentType = (PaymentPreferences.PaymentMethod) modelMap.get("preferedPaymentType");
        assertThat(preferredPaymentType, is(PaymentPreferences.PaymentMethod.TRIALPAY));
    }

    @Test
    public void whenRequestingPaymentViewWithRequestPaymentTypeNullThenPreferredPreferencesPaymentTypeIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));
        // AND player's preferred payment details
        given(playerService.getPaymentPreferences(PLAYER_ID)).willReturn(
                new PaymentPreferences(Currency.USD, PAYPAL, "US"));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        PaymentPreferences.PaymentMethod preferredPaymentType = (PaymentPreferences.PaymentMethod) modelMap.get("preferedPaymentType");
        assertThat(preferredPaymentType, is(PAYPAL));
    }

    @Test
    public void whenRequestingPaymentViewWithRequestPaymentTypeNullAndNoPreferredPreferencesThenCreditCardIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        PaymentPreferences.PaymentMethod preferredPaymentType = (PaymentPreferences.PaymentMethod) modelMap.get("preferedPaymentType");
        assertThat(preferredPaymentType, is(PaymentPreferences.PaymentMethod.CREDITCARD));
    }

    @Test
    public void whenRequestingPaymentViewWithRequestGameTypeThenItIsAddedToModel() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, "BLACKJACK", null);

        String gametype = (String) modelMap.get("gameType");
        assertThat(gametype, is("BLACKJACK"));
    }

    @Test
    public void whenRequestingPaymentViewWithRequestGameTypeThenPaymentGameTypeCookieIsWritten() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, "BLACKJACK", null);

        verify(cookieHelper).setPaymentGameType(httpServletResponse, "BLACKJACK");
    }

    @Test
    public void whenRequestingPaymentViewWithRequestGameTypeNullThenGameTypeIsReadFromCookie() {
        // GIVEN there is an active session
        given(lobbySessionCache.getActiveSession(httpServletRequest)).willReturn(
                new LobbySession(SESSION_ID, PLAYER_ID, null, null, null, null, null, null, true, WEB, AuthProvider.YAZINO));

        // WHEN requesting the payment details
        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, null, null);

        verify(cookieHelper).getLastGameType(httpServletRequest.getCookies(), siteConfiguration.getDefaultGameType());
    }

    @Test
    public void processRequestIsValidWithOutPromotionDetails() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(true);

        boolean valid = underTest.validateProcessRequest(PLAYER_ID,
                "paypal",
                "optionId",
                null,
                null,
                httpServletRequest,
                httpServletResponse);
        assertTrue(valid);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenRequestingPaymentViewForFBShouldModifyPaymentOptionsIds() {
        when(cookieHelper.isOnCanvas(httpServletRequest, httpServletResponse)).thenReturn(true);
        when(facebookCurrencyService.getPreferredCurrencyFromFacebook(httpServletRequest, "HIGH_STAKES")).thenReturn(Currency.GBP);
        mockPromotionService();

        ModelMap modelMap = new ModelMap();
        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, "HIGH_STAKES", null);

        Map<Currency, List<PaymentOption>> paymentOptions = (Map<Currency, List<PaymentOption>>) modelMap.get("paymentOptions");
        final List<PaymentOption> gbpOptions = paymentOptions.get(Currency.GBP);
        assertThat(gbpOptions.get(0).getPromotion(FACEBOOK).getPromoId(), is(666l));
        assertThat(gbpOptions.get(0).getId(), CoreMatchers.equalTo("gbp1_3_buys_20k_666"));
    }

    private Map<Currency, List<PaymentOption>> mockPromotionService() {
        Map<Currency, List<PaymentOption>> expectedPaymentOptions = newHashMap();
        expectedPaymentOptions.put(Currency.USD, Arrays.asList(
                paymentOption("1", FACEBOOK, valueOf(100), valueOf(200), "USD", valueOf(10)),
                paymentOption("2", FACEBOOK, valueOf(10000), valueOf(2000), "USD", valueOf(100)),
                paymentOption("3", FACEBOOK, valueOf(100000), valueOf(20000), "USD", valueOf(1000))));
        expectedPaymentOptions.put(Currency.CAD, Arrays.asList(
                paymentOption("1", FACEBOOK, valueOf(10), valueOf(10), "CAD", null),
                paymentOption("2", FACEBOOK, valueOf(1000), valueOf(100), "CAD", null),
                paymentOption("3", FACEBOOK, valueOf(10000), valueOf(1000), "CAD", null)));
        expectedPaymentOptions.put(Currency.GBP, Arrays.asList(
                paymentOption("optionGBP1", FACEBOOK, valueOf(10000), valueOf(20000), "GBP", new BigDecimal(3.00)),
                paymentOption("optionGBP2", FACEBOOK, valueOf(21000), valueOf(42000), "GBP", new BigDecimal(5.00)),
                paymentOption("optionGBP3", FACEBOOK, valueOf(50000), valueOf(100000), "GBP", new BigDecimal(10.00))));
        when(promotionService.getBuyChipsPaymentOptionsFor(PLAYER_ID, FACEBOOK_CANVAS)).thenReturn(expectedPaymentOptions);
        return expectedPaymentOptions;
    }

    @Test
    public void gbpFbCurrencyShouldResolveToGbpPackages() {
        ModelMap modelMap = new ModelMap();
        when(facebookCurrencyService.getPreferredCurrencyFromFacebook(httpServletRequest, "HIGH_STAKES")).thenReturn(Currency.GBP);

        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, "HIGH_STAKES", null);

        assertThat((String) modelMap.get("preferredCurrency"), is(CoreMatchers.equalTo("GBP")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullAccessTokenShouldHaveCorrectDefaultPrices() {
        final ModelMap modelMap = new ModelMap();
        mockPromotionService();
        when(cookieHelper.isOnCanvas(httpServletRequest, httpServletResponse)).thenReturn(true);
        when(facebookCurrencyService.getPreferredCurrencyFromFacebook(httpServletRequest, "HIGH_STAKES")).thenReturn(Currency.USD);

        underTest.view(modelMap, httpServletRequest, httpServletResponse, null, "HIGH_STAKES", null);

        final Map<Currency, List<PaymentOption>> paymentOptionsMap = (Map<Currency, List<PaymentOption>>) modelMap.get("paymentOptions");
        List<PaymentOption> usdPaymentOptions = paymentOptionsMap.get(Currency.USD);
        assertThat((usdPaymentOptions.get(0)).getAmountRealMoneyPerPurchase(), is(comparesEqualTo(BigDecimal.valueOf(10))));
        assertThat((usdPaymentOptions.get(1)).getAmountRealMoneyPerPurchase(), comparesEqualTo(BigDecimal.valueOf(100)));
        assertThat((usdPaymentOptions.get(2)).getAmountRealMoneyPerPurchase(), comparesEqualTo(BigDecimal.valueOf(1000)));

    }

    private List<PaymentOption> assertThatAllPaymentOptionsAreUSD(final ModelMap modelMap) {
        final Map<Currency, List<PaymentOption>> paymentOptionsMap = (Map<Currency, List<PaymentOption>>) modelMap.get("paymentOptions");
        List<PaymentOption> usdPaymentOptions = paymentOptionsMap.get(Currency.USD);

        assertNotNull(usdPaymentOptions);
        return usdPaymentOptions;
    }

    @Test
    public void roundingShouldRoundToCorrectValue() {
        final PaymentOption blah = paymentOption("blah",
                null, new BigDecimal(4.99), new BigDecimal(4.99), "GBP", new BigDecimal(4.99));
        assertThat(underTest.calculateLocalCurrency(new FacebookUserCurrency(10.0, "GBP", 100, 0.1), blah.getAmountRealMoneyPerPurchase()),
                CoreMatchers.equalTo(new BigDecimal("4.99")));
    }


    @Test
    public void processRequestIsInValidWithOutPaymentMethod() {
        boolean valid = underTest.validateProcessRequest(PLAYER_ID, "", "optionId", null, null, httpServletRequest, httpServletResponse);
        assertFalse(valid);
    }

    @Test
    public void processRequestIsInValidWithOutPaymentOption() {
        boolean valid = underTest.validateProcessRequest(PLAYER_ID, "paypal", "", null, null, httpServletRequest, httpServletResponse);
        assertFalse(valid);
    }

    @Test
    public void processRequestThrowsExceptionWithUnknownCashier() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(false);
        assertFalse(underTest.validateProcessRequest(PLAYER_ID, "paypal", "optionId", null, null, httpServletRequest, httpServletResponse));
    }

    @Test
    public void processRequestThrowsExceptionWithMissingPromoId() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(false);
        assertFalse(underTest.validateProcessRequest(PLAYER_ID,
                "paypal",
                "optionId",
                null,
                BigDecimal.TEN,
                httpServletRequest,
                httpServletResponse));
    }

    @Test
    public void processRequestThrowsExceptionWithUnknownMissingPromoChips() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(false);
        assertFalse(underTest.validateProcessRequest(PLAYER_ID,
                "paypal",
                "optionId",
                PROMO_ID,
                null,
                httpServletRequest,
                httpServletResponse));
    }

    @Test
    public void processRequestThrowsExceptionWithUnknownPromoId() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(true);
        given(promotionService.getPaymentOptionFor(PLAYER_ID, PROMO_ID, PAYPAL, "optionId")).willReturn(null);
        assertFalse(underTest.validateProcessRequest(PLAYER_ID, "paypal", "optionId", PROMO_ID, BigDecimal.ONE, httpServletRequest,
                httpServletResponse));
    }

    @Test
    public void processRequestThrowsExceptionWhenRequestPromoChipsDiffersFromChipsInPromotion() {
        given(cashierConfigurationContainer.cashierExists("paypal")).willReturn(true);
        PaymentOption paymentOption = new PaymentOption();
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(PAYPAL, PROMO_ID, BigDecimal.TEN, "", "");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        given(promotionService.getPaymentOptionFor(PLAYER_ID, PROMO_ID, PAYPAL, "optionId")).willReturn(paymentOption);
        assertFalse(underTest.validateProcessRequest(PLAYER_ID,
                "paypal",
                "optionId",
                PROMO_ID,
                BigDecimal.ONE,
                httpServletRequest,
                httpServletResponse));
    }

    @Test
    public void shouldGenerateCashierRedirect() throws IOException {
        final ModelAndView modelAndView = underTest.process("paypal", "optionId", PROMO_ID, BigDecimal.ONE.toPlainString(),
                httpServletRequest, httpServletResponse);

        final String redirectUrl = (String) modelAndView.getModel().get("redirectUrl");
        assertThat(redirectUrl, is("http://ilo.com?paymentOption=optionId&promoId=666&promoChips=1"));
    }

    @Test
    public void shouldThrowIllegalStateExceptionForInvalidPromoChipAmount() throws IOException {
        assertThat(underTest.process("paypal", "optionId", 1l, "anInvalidAmount",
                httpServletRequest, httpServletResponse), is(CoreMatchers.equalTo(null)));
    }

    @Test
    public void shouldPopulateCardIdsForPlayer() {
        RegisteredCardQueryResult expected = new RegisteredCardsQueryResultBuilder().build();
        when(worldPayCreditCardQueryService.retrieveCardsFor(any(BigDecimal.class))).thenReturn(expected);
        ModelAndView modelAndView = underTest.view(new ModelMap(), httpServletRequest, httpServletResponse, null, null, null);

        RegisteredCardQueryResult registeredCardQueryResult = (RegisteredCardQueryResult) modelAndView.getModel().get(REGISTERED_CARDS_PARAMETER);
        assertThat("Registered card results", registeredCardQueryResult, is(equalTo(expected)));
    }

    private PaymentOption paymentOption(final String optionId,
                                        PaymentPreferences.PaymentMethod paymentMethod,
                                        BigDecimal chipsPerPurchase,
                                        BigDecimal promotionChipsPerPurchase,
                                        String currencyCode,
                                        final BigDecimal amountOfMoney) {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId(optionId);
        paymentOption.setCurrencyCode(currencyCode);
        paymentOption.setAmountRealMoneyPerPurchase(amountOfMoney);
        paymentOption.setNumChipsPerPurchase(chipsPerPurchase);
        PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod, PROMO_ID,
                promotionChipsPerPurchase, "", "");
        paymentOption.addPromotionPaymentOption(promotionPaymentOption);
        return paymentOption;
    }

    private CashierConfiguration cashierConfiguration() {
        CashierConfiguration cashierConfiguration = new CashierConfiguration();
        cashierConfiguration.setCashierUrl("http://ilo.com");
        cashierConfiguration.setCashierId("paypal");
        return cashierConfiguration;
    }

}
