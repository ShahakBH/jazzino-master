package com.yazino.web.payment.googlecheckout;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleCheckoutControllerTest {
    private static final String EMAIL = "email";
    private static final String PLAYER_NAME = "Godot Bardot";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "SLOTS";
    private static final String ORDER_NUMBER = "456";
    private static final String CURRENCY_CODE = "USD";
    private static final BigDecimal CHIPS = BigDecimal.valueOf(10000);
    private static final BigDecimal PRICE = BigDecimal.valueOf(1.5);
    private static final String PRODUCT_ID = "prod id";
    private static final Order EXPECTED_ORDER;
    private static final List<String> EXPECTED_AVAILABLE_PRODUCTS = Arrays.asList("product 1", "product 2");
    public static final String MERCHANT_ORDER_NUMBER = "12999763169054705758.13040412228857433";
    public static final BigDecimal CHIPS_FOR_MERCHANT_ORDER = BigDecimal.valueOf(2500);
    public static final BigDecimal PRICE_FOR_MERCHANT_ORDER = BigDecimal.TEN;
    public static final String PRODUCT_ID_FOR_MERCHANT_ORDER = "slots_usd3_buys_5k";
    public static final String MERCHANT_ORDER_NUMBER2 = "12999763169054705758.13040412228857433";
    public static final BigDecimal CHIPS_FOR_MERCHANT_ORDER2 = BigDecimal.valueOf(4345);
    public static final BigDecimal PRICE_FOR_MERCHANT_ORDER2 = BigDecimal.ONE;
    public static final String PRODUCT_ID_FOR_MERCHANT_ORDER2 = "slots_usd6_buys_4.345k";

    static {
        EXPECTED_ORDER = new Order(ORDER_NUMBER, Order.Status.DELIVERED);
        EXPECTED_ORDER.setChips(CHIPS);
        EXPECTED_ORDER.setCurrencyCode(CURRENCY_CODE);
        EXPECTED_ORDER.setPrice(PRICE);
        EXPECTED_ORDER.setProductId(PRODUCT_ID);
    }

    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    @Mock
    AndroidPromotionService androidPromotionService;

    @Mock
    private GoogleCheckoutService googleCheckoutService;

    @Mock
    private AndroidInAppBillingService androidInAppBillingService;

    @Mock
    LobbySessionCache lobbySessionCache;

    @Mock
    private HttpServletRequest request;

    private MockHttpServletResponse response;
    private GoogleCheckoutController underTest;
    private final static Partner partnerId= Partner.YAZINO;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        response = new MockHttpServletResponse();

        underTest = new GoogleCheckoutController(lobbySessionCache, googleCheckoutService, androidPromotionService, androidInAppBillingService);
    }

    @Test
    public void shouldSetContentTypeToJson() {
        setUpExpectedInteractionsForValidLegacyTransaction();
        underTest.completeTransaction(request, response, GAME_TYPE, ORDER_NUMBER, null);
        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    public void completionShouldReturnBadRequestIfGameTypeIsMissing() {
        setUpExpectedInteractionsForValidLegacyTransaction();

        underTest.completeTransaction(request, response, null, ORDER_NUMBER, null);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
    }

    @Test
    public void completionShouldReturnBadRequestIfOrderTypeIsMissing() {
        setUpExpectedInteractionsForValidLegacyTransaction();

        underTest.completeTransaction(request, response, GAME_TYPE, null, null);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
    }

    @Test
    public void shouldLogCompleteTransactionRequestWithNullOrderJSON() {
        setUpExpectedInteractionsForValidLegacyTransaction();
        final ListAppender logAppender = ListAppender.addTo(GoogleCheckoutController.class);

        underTest.completeTransaction(request, response, GAME_TYPE, ORDER_NUMBER, null);

        Matcher<Iterable<? super String>> hasItemResult = hasItem("Verifying and fulfilling Google Checkout transaction for order 456 and gameType SLOTS. order json is: null");
        assertThat((Iterable<? super String>) logAppender.getMessages(), hasItemResult);
    }

    @Test
    public void shouldLogCompleteTransactionRequest() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        final ListAppender logAppender = ListAppender.addTo(GoogleCheckoutController.class);

        underTest.completeTransaction(request, response, GAME_TYPE, ORDER_NUMBER, "{some order json}");

        Matcher<Iterable<? super String>> hasItemResult = hasItem("Verifying and fulfilling Google Checkout transaction for order 456 and gameType SLOTS. order json is: {some order json}");
        assertThat((Iterable<? super String>) logAppender.getMessages(), hasItemResult);
    }

    @Test
    public void shouldWriteSetStatusToForbiddenWhenNoLobbySession() {
        underTest.completeTransaction(request, response, GAME_TYPE, ORDER_NUMBER, null);
        Assert.assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    public void shouldWriteJsonOrderWhenCompletingTransaction() throws UnsupportedEncodingException {
        setUpExpectedInteractionsForValidLegacyTransaction();

        underTest.completeTransaction(request, response, GAME_TYPE, ORDER_NUMBER, null);

        String actualJson = response.getContentAsString();
        // cannot rely on ordering, so deserialize
        final Order order = new JsonHelper().deserialize(Order.class, actualJson);
        assertThat(order, is(EXPECTED_ORDER));
    }

    @Test
    public void shouldFulfillBuyChipsOrderWhenMerchantOrderNumberReceived() throws UnsupportedEncodingException {
        setUpExpectedInteractionsForValidMerchantOrderNumberTransaction();

        String orderJSON = buildOrderJsonWithMerchantOrderNumber();
        final HashMap<String, String> hashMap = new JsonHelper().deserialize(HashMap.class, orderJSON);

        underTest.completeTransaction(request, response, GAME_TYPE, MERCHANT_ORDER_NUMBER, orderJSON);

        String actualJson = response.getContentAsString();
        // cannot rely on ordering, so deserialize
        final Order order = new JsonHelper().deserialize(Order.class, actualJson);
        Order expectedOrder = new Order(MERCHANT_ORDER_NUMBER, Order.Status.DELIVERED);
        expectedOrder.setChips(CHIPS_FOR_MERCHANT_ORDER);
        expectedOrder.setCurrencyCode(CURRENCY_CODE);
        expectedOrder.setPrice(PRICE_FOR_MERCHANT_ORDER);
        expectedOrder.setProductId(PRODUCT_ID_FOR_MERCHANT_ORDER);

        assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldReturnFirstOrderWhenMultipleGoogleBillingTransactionsReceived() throws UnsupportedEncodingException {
        setUpExpectedInteractionsForMultipleMerchantOrderNumberTransactions();

        String orderJSON = buildOrderJsonWithMerchantOrderNumber();
        final HashMap<String, String> hashMap = new JsonHelper().deserialize(HashMap.class, orderJSON);

        underTest.completeTransaction(request, response, GAME_TYPE, MERCHANT_ORDER_NUMBER, orderJSON);

        String actualJson = response.getContentAsString();
        // cannot rely on ordering, so deserialize
        final Order order = new JsonHelper().deserialize(Order.class, actualJson);
        Order expectedOrder = new Order(MERCHANT_ORDER_NUMBER, Order.Status.DELIVERED);
        expectedOrder.setChips(CHIPS_FOR_MERCHANT_ORDER);
        expectedOrder.setCurrencyCode(CURRENCY_CODE);
        expectedOrder.setPrice(PRICE_FOR_MERCHANT_ORDER);
        expectedOrder.setProductId(PRODUCT_ID_FOR_MERCHANT_ORDER);

        assertThat(order, is(expectedOrder));
    }

    private String buildOrderJsonWithMerchantOrderNumber() {
        return "{\"nonce\":-125345462236698780,\"orders\":[{\"notificationId\":\"3288054527729\",\"orderId\":\""
                + MERCHANT_ORDER_NUMBER + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
                + PRODUCT_ID_FOR_MERCHANT_ORDER
                + "\",\"purchaseTime\":1354771860000,\"purchaseState\":0,\"purchaseToken\":\"xnhxsjkiyekdhquchwshhjwy\"}]}";
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWriteJsonProductIdsWhenRequestingAvailableProducts() throws UnsupportedEncodingException {
        setUpExpectedInteractionsForProducts(EXPECTED_AVAILABLE_PRODUCTS);

        String actualProductsJson = underTest.fetchAvailableProducts(request, response, GAME_TYPE);

        final List<String> products = new JsonHelper().deserialize(List.class, actualProductsJson);
        assertThat(products, is(EXPECTED_AVAILABLE_PRODUCTS));
    }

    @Test
    public void shouldWriteEmptyJsonWhenNoProductsAvailable() throws UnsupportedEncodingException {
        setUpExpectedInteractionsForProducts(Collections.<String>emptyList());

        String actualProductsJson = underTest.fetchAvailableProducts(request, response, GAME_TYPE);

        assertThat(actualProductsJson, is("[]"));
    }

    @Test
    public void productListShouldReturnBadRequestWhenGameTypeIsMissing() throws UnsupportedEncodingException {
        String actualProductsJson = underTest.fetchAvailableProducts(request, response, null);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(actualProductsJson, is(nullValue()));
    }

    @Test
    public void fetchBuyChipStoreShouldReturnBadRequestIfGameTypeIsMissing() throws IOException {
        underTest.fetchBuyChipStoreProducts(request, response, null);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    public void fetchBuyChipStoreShouldWriteEmptyJsonWhenNoProductsAvailable() throws IOException {
        setUpExpectedInteractionsForValidLegacyTransaction();
        underTest.fetchBuyChipStoreProducts(request, response, GAME_TYPE);
        assertThat(response.getContentAsString(), is("{}"));
    }

    @Test
    public void fetchBuyChipStoreProductsShouldSetContentTypeToJson() throws IOException {
        setUpExpectedInteractionsForValidLegacyTransaction();
        underTest.fetchBuyChipStoreProducts(request, response, GAME_TYPE);
        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    public void fetchBuyChipStoreProductsShouldRequestProductsFromGoogleBuyChipStoreService() throws IOException {
        setUpExpectedInteractionsForProducts(Collections.<String>emptyList());
        underTest.fetchBuyChipStoreProducts(request, response, GAME_TYPE);
        verify(androidPromotionService).getBuyChipStoreConfig(PLAYER_ID, Platform.ANDROID, GAME_TYPE);
    }

    @Test
    // fix for client sometimes wrapping JSON in quotes - seems to be when app loads transactions from cookies after flash automatically flushes orders
    public void shouldStripLeadingAndTrailingQuoteFromJSONOrder() {
        setUpExpectedInteractionsForValidMerchantOrderNumberTransaction();
        final String validJSON = buildOrderJsonWithMerchantOrderNumber();

        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, SESSION_ID, PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        underTest.completeTransaction(request, response, GAME_TYPE, MERCHANT_ORDER_NUMBER, "\"" + validJSON + "\"");

        verify(googleCheckoutService).fulfillBuyChipsOrder(paymentContext, validJSON);
    }


    private void setUpExpectedInteractionsForValidLegacyTransaction() {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);

        Order order = new Order(ORDER_NUMBER, Order.Status.DELIVERED);
        order.setChips(CHIPS);
        order.setCurrencyCode(CURRENCY_CODE);
        order.setPrice(PRICE);
        order.setProductId(PRODUCT_ID);
        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, session.getSessionId(), PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        when(googleCheckoutService.fulfillLegacyBuyChipsOrder(paymentContext, order.getOrderNumber())).thenReturn(order);
    }

    private void setUpExpectedInteractionsForValidMerchantOrderNumberTransaction() {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);

        Order order = new Order(MERCHANT_ORDER_NUMBER, Order.Status.DELIVERED);
        order.setChips(CHIPS_FOR_MERCHANT_ORDER);
        order.setCurrencyCode(CURRENCY_CODE);
        order.setPrice(PRICE_FOR_MERCHANT_ORDER);
        order.setProductId(PRODUCT_ID_FOR_MERCHANT_ORDER);
        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, session.getSessionId(), PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        when(googleCheckoutService.fulfillBuyChipsOrder(paymentContext, buildOrderJsonWithMerchantOrderNumber())).thenReturn(Arrays.asList(order));
    }

    private void setUpExpectedInteractionsForMultipleMerchantOrderNumberTransactions() {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);

        Order order = new Order(MERCHANT_ORDER_NUMBER, Order.Status.DELIVERED);
        order.setChips(CHIPS_FOR_MERCHANT_ORDER);
        order.setCurrencyCode(CURRENCY_CODE);
        order.setPrice(PRICE_FOR_MERCHANT_ORDER);
        order.setProductId(PRODUCT_ID_FOR_MERCHANT_ORDER);
        Order order2 = new Order(MERCHANT_ORDER_NUMBER2, Order.Status.DELIVERED);
        order2.setChips(CHIPS_FOR_MERCHANT_ORDER2);
        order2.setCurrencyCode(CURRENCY_CODE);
        order2.setPrice(PRICE_FOR_MERCHANT_ORDER2);
        order2.setProductId(PRODUCT_ID_FOR_MERCHANT_ORDER2);
        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, session.getSessionId(), PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        when(googleCheckoutService.fulfillBuyChipsOrder(paymentContext, buildOrderJsonWithMerchantOrderNumber())).thenReturn(Arrays.asList(order, order2));
    }

    private void setUpExpectedInteractionsForProducts(List<String> products) {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);

        when(googleCheckoutService.fetchAvailableProducts(GAME_TYPE)).thenReturn(products);
    }

    @Test
    public void shouldLogCompleteOrdersRequest() {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);
        final ListAppender logAppender = ListAppender.addTo(GoogleCheckoutController.class);

        underTest.completeOrders(request, response, GAME_TYPE, "{some order json}", "its signature", "{promoIdsMap}");

        Matcher<Iterable<? super String>> hasItemResult = hasItem("Verifying and completing android orders. gameType: SLOTS, order json is: {some order json}, signature: its signature, promoIds: {promoIdsMap}");
        assertThat((Iterable<? super String>) logAppender.getMessages(), hasItemResult);
    }

    @Test
    public void completeOrders_shouldSetStatusToForbiddenWhenNoActiveLobbySession() {
        underTest.completeOrders(request, response, GAME_TYPE, "{some order json}", "its signature", "{promoIdsMap}");
        Assert.assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    // fix for client sometimes wrapping JSON in quotes - seems to be when app loads transactions from cookies after flash automatically flushes orders
    public void completeOrders_shouldStripLeadingAndTrailingQuoteFromOrderJSON() {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        String orderJSONWithOutQuotes = "{some json}";
        String orderJSONWithQuotes = "\"" + orderJSONWithOutQuotes + "\"";

        underTest.completeOrders(request, response, GAME_TYPE, orderJSONWithQuotes, "its signature", "{promoIdsMap}");

        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, session.getSessionId(), PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        verify(androidInAppBillingService).verifyAndCompleteTransactions(paymentContext, orderJSONWithOutQuotes, "its signature", "{promoIdsMap}");
    }

    @Test
    // fix for client sometimes wrapping JSON in quotes
    public void completeOrders_shouldStripLeadingAndTrailingQuoteFromPromoIdsJSON() {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        String promoIdsJSONWithOutQuotes = "{promoIds map}";
        String promoIdsJSONWithQuotes = "\"" + promoIdsJSONWithOutQuotes + "\"";

        underTest.completeOrders(request, response, GAME_TYPE, "{order json}", "its signature", promoIdsJSONWithQuotes);

        PaymentContext paymentContext = new PaymentContext(PLAYER_ID, session.getSessionId(), PLAYER_NAME, GAME_TYPE, EMAIL, null, null, partnerId);
        verify(androidInAppBillingService).verifyAndCompleteTransactions(paymentContext, "{order json}", "its signature", promoIdsJSONWithOutQuotes);
    }

    @Test
    public void completeOrders_shouldWriteOrdersToTheResponse() throws UnsupportedEncodingException {
        LobbySession session = new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "", Partner.YAZINO, "", EMAIL, null, true, WEB, AuthProvider.YAZINO);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(session);
        final VerifiedOrder order1 = new VerifiedOrderBuilder()
                .withOrderId("123.456")
                .withProductId("product1")
                .withStatus(OrderStatus.DELIVERED)
                .withChips(BigDecimal.TEN)
                .withDefaultChips(BigDecimal.TEN)
                .withPrice(BigDecimal.valueOf(5677))
                .withCurrencyCode("USD").buildVerifiedOrder();
        final VerifiedOrder order2 = new VerifiedOrderBuilder()
                .withOrderId("987.654")
                .withProductId("product2_isunknown")
                .withStatus(OrderStatus.ERROR).buildVerifiedOrder();
        final List<VerifiedOrder> orders = Arrays.asList(order1, order2);
        when(androidInAppBillingService.verifyAndCompleteTransactions(
                Mockito.<PaymentContext>any(), anyString(), anyString(), anyString())).thenReturn(orders);

        underTest.completeOrders(request, response, GAME_TYPE, "order json", "signature", "promoIds");

        final String actualJsonResponse = response.getContentAsString();
        final String expectedOrders = new JsonHelper().serialize(orders);

        assertThat(actualJsonResponse, is(expectedOrders));
    }
}
