package com.yazino.web.payment.facebook;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.facebook.SignedRequest;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FacebookCashierControllerTest {

    private static final String OPTION_USD3 = "optionUSD3";
    private static final BigDecimal PLAYER_ID = new BigDecimal(112);
    private static final String BUYER_ID = "101111111111111";
    private static final String PAYMENTS_GET_ITEM_PRICE = "payments_get_item_price";
    private static final String FACEBOOK_USER_ID = "xyz123";
    private static final String PAYMENT_ID = "123";
    private static final String GAME_TYPE = "HIGH_STAKES";
    private static final long PROMO_ID = 666l;
    private static final String PRODUCT_URL = "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_25_buys_100k_" + PROMO_ID;
    private static final BigDecimal AMOUNT = valueOf(100l);
    private static final String REQUEST_ID = "internal_request_id";
    private static final String CONFIRM_PAYMENT_REQUEST_BODY = "{\"object\":\"payments\",\"entry\":[{\"id\":\"123\"}]}\"}";
    private static final String DISPUTE_PAYMENT_REQUEST_BODY = "{\"object\":\"payments\",\"entry\":[{\"id\":\"123\",\"changed_fields\":[\"disputes\"]}]}\"}";
    private static final String CHARGEBACK_PAYMENT_REQUEST_BODY = "{\"object\":\"payments\",\"entry\":[{\"id\":\"123\",\"changed_fields\":[\"disputes\"]}]}\"}";
    private static final DateTime DISPUTE_DATE = new DateTime(2014, 1, 3, 12, 0, 0);

    private FacebookPaymentService facebookService = mock(FacebookPaymentService.class);
    private SiteConfiguration siteConfiguration = mock(SiteConfiguration.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private HttpServletResponse response;
    private FacebookConfiguration facebookConfiguration = mock(FacebookConfiguration.class);
    private SignedRequestValidator signedRequestValidator = mock(SignedRequestValidator.class);
    private YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
    private FacebookPaymentIntegrationService facebookPaymentIntegrationService = mock(FacebookPaymentIntegrationService.class);
    private SignedRequest mockSignedRequest = mock(SignedRequest.class);

    private FacebookCashierController underTest;

    @Before
    public void setUp() throws IOException {
        underTest = new FacebookCashierController(facebookService, facebookConfiguration, signedRequestValidator,
                lobbySessionCache, facebookPaymentIntegrationService) {
            @Override
            protected SignedRequest getSignedRequest(final String signedRequest, final FacebookAppConfiguration appConfigForGameType) {
                return mockSignedRequest;
            }
        };
        response = mock(HttpServletResponse.class);
        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        final FacebookAppConfiguration applicationConfig = mock(FacebookAppConfiguration.class);
        when(mockSignedRequest.get("user_id")).thenReturn("MY PRECIOUS");
        final Map<String, String> mockPayment = mock(Map.class);
        when(mockSignedRequest.get("payment")).thenReturn(mockPayment);
        when(mockPayment.get("request_id")).thenReturn("unique_id");
        when(applicationConfig.getSecretKey()).thenReturn("key");
        when(yazinoConfiguration.getString("facebook.accessToken.HIGH_STAKES")).thenReturn("accessToken");
        when(facebookConfiguration.getAppConfigFor("HIGH_STAKES", FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT)).thenReturn(applicationConfig);
        when(siteConfiguration.getAssetUrl()).thenReturn("https://jrae-centos.london.yazino.com:8143/web-content");
        LobbySession lobbySession = mock(LobbySession.class);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
    }

    @Test
    public void getModifiedDataShouldDeserialiseEncodedEarnChipsData() {

        String facebookTestJson = "{" +
                "\"buyer\":" + BUYER_ID + "," +
                "\"items\":" +
                "[{" +
                "\"data\":\"{" +
                "\\\"modified\\\":{\\\"product\\\":\\\"http:\\/\\/env-proxy.london.yazino.com\\/jrae-centos\\/fbog\\/currency\\/HIGH_STAKES_earnchips\\\",\\\"product_title\\\":\\\"Yazino Chips\\\",\\\"product_amount\\\":50000,\\\"credits_amount\\\":200}" +
                "}\"" +
                "}]" +
                "}";
        final Map orderInfoItems = new JsonHelper().deserialize(Map.class, facebookTestJson);

        final Object items = ((List) orderInfoItems.get("items")).get(0);
        final String data = (String) ((Map) items).get("data");
        Map modified = underTest.getEarnChipsData(data);

        assertThat((String) modified.get("product"),
                is(CoreMatchers.equalTo("http://env-proxy.london.yazino.com/jrae-centos/fbog/currency/HIGH_STAKES_earnchips")));
    }

    @Test
    public void productIdWithoutPromoShouldReturnPriceOfNonPromoPackage() throws IOException {
        when(signedRequestValidator.validate(anyString(), anyString())).thenReturn(true);
        when(facebookService.getPlayerId("MY PRECIOUS")).thenReturn(valueOf(123));
        PaymentOption paymentOption = mock(PaymentOption.class);
        when(paymentOption.getAmountRealMoneyPerPurchase()).thenReturn(valueOf(20));
        when(paymentOption.getRealMoneyCurrency()).thenReturn("USD");
        when(paymentOption.getNumChipsPerPurchase("FACEBOOK")).thenReturn(valueOf(100000));
        when(facebookService.resolvePaymentOption(valueOf(123), "optionUSD3", null)).thenReturn(paymentOption);

        final Map<String, Object> jsonResponse = underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_100k", "", "HIGH_STAKES", request);

        final Map<String, Object> content = (Map<String, Object>) jsonResponse.get("content");
        final BigDecimal amount = (BigDecimal) content.get("amount");
        assertThat(amount, comparesEqualTo(valueOf(20l)));
        assertThat((String) content.get("currency"), comparesEqualTo("USD"));
    }

    @Test
    public void invalidProductShouldReturnErrorInGetItemPrice() throws IOException {
        when(signedRequestValidator.validate(anyString(), anyString())).thenReturn(true);

        assertThat(underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/product/usd3_10_buys_100k_8", "", "HIGH_STAKES", request), nullValue());
        assertThat(underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "/fbog/product/usd3_10_buys_100k_8", "", "HIGH_STAKES", request), nullValue());
        assertThat(underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_buys_100k_8", "", "HIGH_STAKES", request), nullValue());
        assertThat(underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_8", "", "HIGH_STAKES", request), nullValue());
        verifyNoMoreInteractions(facebookService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getItemPriceShouldCheckPlayerIsInPromotionAndReturnPriceOfPromoPackage() throws IOException {
        when(signedRequestValidator.validate(anyString(), anyString())).thenReturn(true);
        when(facebookService.getPlayerId("MY PRECIOUS")).thenReturn(PLAYER_ID);
        PaymentOption paymentOption = mock(PaymentOption.class);
        when(paymentOption.getAmountRealMoneyPerPurchase()).thenReturn(valueOf(20));
        when(paymentOption.getRealMoneyCurrency()).thenReturn("USD");
        when(paymentOption.getNumChipsPerPurchase("FACEBOOK")).thenReturn(valueOf(100));
        when(facebookService.resolvePaymentOption(PLAYER_ID, "optionUSD3", 8l)).thenReturn(paymentOption);

        final Map<String, Object> jsonResponse = underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_25_buys_100k_8", "", "HIGH_STAKES", request);

        final Map<String, Object> content = (Map<String, Object>) jsonResponse.get("content");
        final BigDecimal amount = (BigDecimal) content.get("amount");
        assertThat(amount, comparesEqualTo(valueOf(20l)));
        assertThat((String) content.get("currency"), comparesEqualTo("USD"));
        verify(facebookService).logTransactionAttempt(PLAYER_ID, "unique_id", paymentOption, "HIGH_STAKES", 8l);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getItemPriceShouldReturnPriceOfPromoPackageForAUD() throws IOException {
        when(signedRequestValidator.validate(anyString(), anyString())).thenReturn(true);
        when(facebookService.getPlayerId("MY PRECIOUS")).thenReturn(PLAYER_ID);
        PaymentOption paymentOption = mock(PaymentOption.class);
        when(paymentOption.getAmountRealMoneyPerPurchase()).thenReturn(valueOf(19.40));
        when(paymentOption.getRealMoneyCurrency()).thenReturn("AUD");
        when(paymentOption.getNumChipsPerPurchase("FACEBOOK")).thenReturn(valueOf(50));
        when(facebookService.resolvePaymentOption(PLAYER_ID, "optionAUD3", null)).thenReturn(paymentOption);

        final Map<String, Object> jsonResponse = underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE,
                "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/aud3_19.40_buys_50k", "", "HIGH_STAKES", request);

        final Map<String, Object> content = (Map<String, Object>) jsonResponse.get("content");
        final BigDecimal amount = (BigDecimal) content.get("amount");
        assertThat(amount, comparesEqualTo(valueOf(19.40)));
        assertThat((String) content.get("currency"), comparesEqualTo("AUD"));
        verify(facebookService).logTransactionAttempt(PLAYER_ID, "unique_id", paymentOption, "HIGH_STAKES", null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getItemPriceForInvalidPromoDetailsShouldReturnDefaults() throws IOException {
        when(signedRequestValidator.validate(anyString(), anyString())).thenReturn(true);
        when(facebookService.getPlayerId("MY PRECIOUS")).thenReturn(valueOf(123));
        PaymentOption paymentOption = mock(PaymentOption.class);
        when(paymentOption.getAmountRealMoneyPerPurchase()).thenReturn(valueOf(20));
        when(paymentOption.getRealMoneyCurrency()).thenReturn("USD");
        when(paymentOption.getNumChipsPerPurchase("FACEBOOK")).thenReturn(valueOf(100000l));
        final PromotionPaymentOption promo = mock(PromotionPaymentOption.class);
        when(promo.getPromotionChipsPerPurchase()).thenReturn(valueOf(100000));
        when(promo.getPromoId()).thenReturn(8l);
        when(paymentOption.getPromotion(PaymentPreferences.PaymentMethod.FACEBOOK)).thenReturn(promo);
        when(facebookService.resolvePaymentOption(valueOf(123), "optionUSD3", 8l)).thenReturn(paymentOption);

        final String invalidProductUrl = "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_25_buys_500k_8";
        final String validProductUrl = "http://env-proxy.london.yazino.com/jrae-centos/fbog/product/usd3_20_buys_100k_8";

        final Map<String, Object> jsonResponse = underTest.getItemPrice(PAYMENTS_GET_ITEM_PRICE, invalidProductUrl, "", "HIGH_STAKES", request);

        final Map<String, Object> content = (Map<String, Object>) jsonResponse.get("content");
        final BigDecimal amount = (BigDecimal) content.get("amount");
        final String product = (String) content.get("product");

        assertThat(product, is(equalTo(validProductUrl)));
        assertThat(amount, comparesEqualTo(valueOf(20l)));
        assertThat((String) content.get("currency"), comparesEqualTo("USD"));
    }

    //need to log init attempt. prolly not here tho. unless in get price

    @Test
    public void confirmPaymentShouldCallCompletePurchase() throws IOException, WalletServiceException {
        final FacebookPayment payment = mock(FacebookPayment.class);
        final PaymentOption paymentOption = mock(PaymentOption.class);
        when(payment.getFacebookUserId()).thenReturn(FACEBOOK_USER_ID);
        when(payment.getProductId()).thenReturn(OPTION_USD3);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getCurrencyCode()).thenReturn("USD");
        when(payment.getAmount()).thenReturn(AMOUNT);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getStatus()).thenReturn(FacebookPayment.Status.completed);
        when(payment.getType()).thenReturn(FacebookPayment.Type.charge);
        when(payment.getRequestId()).thenReturn(REQUEST_ID);
        when(facebookPaymentIntegrationService.retrievePayment(GAME_TYPE, PAYMENT_ID)).thenReturn(payment);

        when(facebookService.getPlayerId(FACEBOOK_USER_ID)).thenReturn(PLAYER_ID);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);

        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        underTest.confirmPayment(response, CONFIRM_PAYMENT_REQUEST_BODY, "HIGH_STAKES");

        verify(facebookService).completePurchase(PLAYER_ID, GAME_TYPE, paymentOption, PAYMENT_ID, REQUEST_ID, "USD", AMOUNT, PROMO_ID);
    }

    @Test
    public void confirmPaymentShouldCallDisputePurchaseIfDisputeHasChangedAndInfoIsPresent() throws IOException, WalletServiceException {
        final FacebookPayment payment = mock(FacebookPayment.class);
        final PaymentOption paymentOption = mock(PaymentOption.class);
        when(payment.getFacebookUserId()).thenReturn(FACEBOOK_USER_ID);
        when(payment.getProductId()).thenReturn(OPTION_USD3);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getCurrencyCode()).thenReturn("USD");
        when(payment.getAmount()).thenReturn(AMOUNT);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getStatus()).thenReturn(FacebookPayment.Status.completed);
        when(payment.getType()).thenReturn(FacebookPayment.Type.charge);
        when(payment.getRequestId()).thenReturn(REQUEST_ID);
        when(payment.getDisputeReason()).thenReturn("aDisputeReason");
        when(payment.getDisputeDate()).thenReturn(DISPUTE_DATE);
        when(facebookPaymentIntegrationService.retrievePayment(GAME_TYPE, PAYMENT_ID)).thenReturn(payment);

        when(facebookService.getPlayerId(FACEBOOK_USER_ID)).thenReturn(PLAYER_ID);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);

        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        underTest.confirmPayment(response, DISPUTE_PAYMENT_REQUEST_BODY, "HIGH_STAKES");

        verify(facebookService).disputePurchase(REQUEST_ID, PAYMENT_ID, PLAYER_ID, paymentOption, GAME_TYPE, PROMO_ID, "aDisputeReason", DISPUTE_DATE);
    }

    @Test
    public void confirmPaymentShouldNotCallDisputePurchaseIfDisputeHasChangedAndInfoIsMissing() throws IOException, WalletServiceException {
        final FacebookPayment payment = mock(FacebookPayment.class);
        final PaymentOption paymentOption = mock(PaymentOption.class);
        when(payment.getFacebookUserId()).thenReturn(FACEBOOK_USER_ID);
        when(payment.getProductId()).thenReturn(OPTION_USD3);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getCurrencyCode()).thenReturn("USD");
        when(payment.getAmount()).thenReturn(AMOUNT);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getStatus()).thenReturn(FacebookPayment.Status.completed);
        when(payment.getType()).thenReturn(FacebookPayment.Type.charge);
        when(payment.getRequestId()).thenReturn(REQUEST_ID);
        when(payment.getDisputeReason()).thenReturn(null);
        when(payment.getDisputeDate()).thenReturn(null);
        when(facebookPaymentIntegrationService.retrievePayment(GAME_TYPE, PAYMENT_ID)).thenReturn(payment);

        when(facebookService.getPlayerId(FACEBOOK_USER_ID)).thenReturn(PLAYER_ID);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);

        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        underTest.confirmPayment(response, DISPUTE_PAYMENT_REQUEST_BODY, "HIGH_STAKES");

        verify(facebookService, times(0)).disputePurchase(anyString(), anyString(), any(BigDecimal.class), any(PaymentOption.class), anyString(), anyLong(), anyString(), any(DateTime.class));
    }

    @Test
    public void chargebackShouldNotCompletePurchaseButShouldDoSomethingElse() throws IOException, WalletServiceException {
        final FacebookPayment payment = mock(FacebookPayment.class);
        final PaymentOption paymentOption = mock(PaymentOption.class);
        when(payment.getFacebookUserId()).thenReturn(FACEBOOK_USER_ID);
        when(payment.getProductId()).thenReturn(OPTION_USD3);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getCurrencyCode()).thenReturn("USD");
        when(payment.getAmount()).thenReturn(AMOUNT);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getStatus()).thenReturn(FacebookPayment.Status.completed);
        when(payment.getType()).thenReturn(FacebookPayment.Type.chargeback);
        when(payment.getRequestId()).thenReturn(REQUEST_ID);
        when(facebookPaymentIntegrationService.retrievePayment(GAME_TYPE, PAYMENT_ID)).thenReturn(payment);

        when(facebookService.getPlayerId(FACEBOOK_USER_ID)).thenReturn(PLAYER_ID);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);

        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        underTest.confirmPayment(response, CHARGEBACK_PAYMENT_REQUEST_BODY, "HIGH_STAKES");

        verify(facebookService).getPlayerId(FACEBOOK_USER_ID);
        verify(facebookService).resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID);

        verifyNoMoreInteractions(facebookService);
    }

    @Test
    public void PaymentFailedShouldLogFailure() throws WalletServiceException, IOException {
        final FacebookPayment payment = mock(FacebookPayment.class);
        final PaymentOption paymentOption = mock(PaymentOption.class);
        when(payment.getFacebookUserId()).thenReturn(FACEBOOK_USER_ID);
        when(payment.getProductId()).thenReturn(OPTION_USD3);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getCurrencyCode()).thenReturn("USD");
        when(payment.getAmount()).thenReturn(AMOUNT);
        when(payment.getPromoId()).thenReturn(PROMO_ID);
        when(payment.getRequestId()).thenReturn(REQUEST_ID);
        when(payment.getStatus()).thenReturn(FacebookPayment.Status.failed);
        when(payment.getType()).thenReturn(FacebookPayment.Type.charge);
        when(facebookPaymentIntegrationService.retrievePayment(GAME_TYPE, PAYMENT_ID)).thenReturn(payment);

        when(facebookService.getPlayerId(FACEBOOK_USER_ID)).thenReturn(PLAYER_ID);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);

        final PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        underTest.confirmPayment(response, CONFIRM_PAYMENT_REQUEST_BODY, "HIGH_STAKES");

        verify(facebookService).logFailedTransaction(null, "Received notification for charge with failed status", PLAYER_ID, PAYMENT_ID, REQUEST_ID, paymentOption, GAME_TYPE, PROMO_ID);
    }

    @Test
    public void shouldLogPurchaseCancellation() throws IOException, WalletServiceException {
        PaymentOption paymentOption = mock(PaymentOption.class);
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(paymentOption);
        underTest.logFailedPurchase(request, response, GAME_TYPE, PRODUCT_URL, REQUEST_ID, "2323", "error message");
        verify(facebookService).logFailedTransaction("2323", "error message", PLAYER_ID, null, REQUEST_ID, paymentOption, GAME_TYPE, PROMO_ID);
    }

    @Test
    public void shouldNotLogFailedPurchaseForNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        underTest.logFailedPurchase(request, response, GAME_TYPE, PRODUCT_URL, REQUEST_ID, "2323", "error message");
        verifyZeroInteractions(facebookService);
    }

    @Test
    public void shouldNotLogFailedPurchaseForInvalidProductUrl() throws IOException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        underTest.logFailedPurchase(request, response, GAME_TYPE, PRODUCT_URL, REQUEST_ID, "2323", "error message");
        verifyZeroInteractions(facebookService);
    }

    @Test
    public void shouldNotLogFailedPurchaseIfPaymentOptionCannotBeResolved() throws IOException, WalletServiceException {
        when(facebookService.resolvePaymentOption(PLAYER_ID, OPTION_USD3, PROMO_ID)).thenReturn(null);
        underTest.logFailedPurchase(request, response, GAME_TYPE, PRODUCT_URL, REQUEST_ID, "2323", "error message");
        verify(facebookService, never()).logFailedTransaction(anyString(), anyString(), any(BigDecimal.class), anyString(), anyString(), any(PaymentOption.class), anyString(), anyLong());
    }

    @Test
    public void earnedChipsShouldReturnNiceString() {
        when(facebookService.getEarnedChipsToday(valueOf(410l))).thenReturn(valueOf(300.00));
        final String earnChipsToday = underTest.getEarnChipsToday(valueOf(410l));
        assertThat(earnChipsToday, is(equalTo("300")));
    }

}
