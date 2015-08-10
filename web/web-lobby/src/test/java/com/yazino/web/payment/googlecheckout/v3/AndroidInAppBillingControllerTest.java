package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.web.payment.PurchaseStatus.FAILED;
import static com.yazino.web.payment.PurchaseStatus.STALE_PROMOTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class AndroidInAppBillingControllerTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final String PRODUCT_ID = "sample product id";
    public static final Long PROMO_ID = 987L;
    public static final String GAME_TYPE = "SLOTS";
    public static final String PURCHASE_ID = "sample purchase id";
    public static final String FAILURE_MESSAGE = "sample message - txn cancelled";
    private static final String ORDER_DATA = "sample order data";
    private static final String SIGNATURE = "sample signature";
    public static final String USER_CANCELLED_FALSE = "false";
    public static final String USER_CANCELLED_NULL = null;
    public static final String USER_CANCELLED_NOT_TRUE = "not a boolean value";
    public static final String USER_CANCELLED_TRUE = "true";

    private static final Partner partnerId= Partner.YAZINO;

    @Mock
    private HttpServletRequest request;
    @Mock
    LobbySessionCache lobbySessionCache;
    @Mock
    AndroidInAppBillingServiceV3 androidInAppBillingService;
    @Mock
    AndroidPromotionServiceV3 androidPromotionService;

    @Mock
    WebApiResponses jsonWriter;
    private MockHttpServletResponse response;
    private AndroidInAppBillingController underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new AndroidInAppBillingController(lobbySessionCache, androidInAppBillingService, jsonWriter, androidPromotionService);
        response = new MockHttpServletResponse();
    }

    @Test
    public void requestPurchaseShouldReturnBadRequestWhenPlayerIdIsMissing() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);

        underTest.requestPurchase(request, response, null, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        assertThat(response.getStatus(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void requestPurchaseShouldReturnBadRequestWhenGameTypeIsMissing() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);

        underTest.requestPurchase(request, response, PLAYER_ID, null, PRODUCT_ID, PROMO_ID.toString());

        assertThat(response.getStatus(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void requestPurchaseShouldReturnBadRequestWhenProductIdIsMissing() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, null, PROMO_ID.toString());

        assertThat(response.getStatus(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void requestPurchaseShouldReturnUnauthorizedWhenNoSession() throws IOException, PurchaseException {

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(jsonWriter, times(1)).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
    }

    @Test
    public void requestPurchaseShouldReturnUnauthorizedWhenSessionIsForDifferentPlayer() throws IOException, PurchaseException {
        BigDecimal playerIdWithSession = BigDecimal.TEN;
        setUpActiveSessionForPlayer(playerIdWithSession);

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(jsonWriter, times(1)).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - player id and session player id mismatch");
    }

    @Test
    public void requestPurchaseShouldMapNullPromotionIdToNullLong() throws PurchaseException, IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);

        verify(androidInAppBillingService, times(1)).createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);
    }

    // TODO remove when clients don't add null to parameter map
    @Test
    public void requestPurchaseShouldMapStringNullPromotionIdToNullLong() throws PurchaseException, IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, "null");

        verify(androidInAppBillingService, times(1)).createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test
    public void requestPurchaseShouldMapEmptyPromotionIdToNullLong() throws PurchaseException, IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, "");

        verify(androidInAppBillingService, times(1)).createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test
    public void requestPurchaseShouldNotCreatePurchaseRequestWhenPromotionIdIsBadlyFormed() throws PurchaseException, IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, "this is not a long");

        verify(jsonWriter).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "cannot convert promotionId (this is not a long) to Long");
        verify(androidInAppBillingService, never()).createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test
    public void requestPurchaseShouldMapValidPromotionIdToLong() throws PurchaseException, IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(androidInAppBillingService, times(1)).createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID);
    }

    @Test
    public void requestPurchaseShouldReturnSTALE_PROMOTIONWhenPromotionIdIsUnknown() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        GooglePurchase purchase = new GooglePurchase();
        purchase.setStatus(STALE_PROMOTION);
        when(androidInAppBillingService.createPurchaseRequest(PLAYER_ID, "SLOTS", PRODUCT_ID, PROMO_ID)).thenReturn(purchase);

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(jsonWriter, times(1)).writeOk(response, purchase);
    }

    @Test
    public void requestPurchaseShouldReturnPurchaseWhenRequestIsCreated() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        GooglePurchase expectedPurchaseRequest = new GooglePurchase();
        when(androidInAppBillingService.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID)).thenReturn(expectedPurchaseRequest);

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(jsonWriter, times(1)).writeOk(response, expectedPurchaseRequest);
    }

    @Test
    public void requestPurchaseShouldSerializeExceptionAsResponseWhenFailure() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        PurchaseException expectedException = new PurchaseException(FAILED, false, "Sample message");
        when(androidInAppBillingService.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID)).thenThrow(expectedException);

        underTest.requestPurchase(request, response, PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID.toString());

        verify(jsonWriter, times(1)).writeOk(response, expectedException);
    }

    @Test
    public void creditPurchaseShouldSerializePurchaseAsResponseWhenSuccess() throws IOException, PurchaseException {
        GooglePurchase expectedPurchase = new GooglePurchase();
        setUpActiveSessionForPlayer(PLAYER_ID);
        when(androidInAppBillingService.creditPurchase(GAME_TYPE, ORDER_DATA, SIGNATURE, partnerId)).thenReturn(expectedPurchase);

        underTest.creditPurchase(request, response, GAME_TYPE, ORDER_DATA, SIGNATURE);

        verify(jsonWriter, times(1)).writeOk(response, expectedPurchase);
    }

    @Test
    public void creditPurchaseShouldSerializeExceptionAsResponseWhenFailure() throws IOException, PurchaseException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        PurchaseException expectedException = new PurchaseException(FAILED, false, "Sample message");
        when(androidInAppBillingService.creditPurchase(GAME_TYPE, ORDER_DATA, SIGNATURE, partnerId)).thenThrow(expectedException);

        underTest.creditPurchase(request, response, GAME_TYPE, ORDER_DATA, SIGNATURE);

        verify(jsonWriter, times(1)).writeOk(response, expectedException);
    }

    @Test
    public void creditPurchaseShouldReturnsUnauthorisedWhenNoSessionIsPresent() throws IOException, PurchaseException {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(null);

        underTest.creditPurchase(request, response, GAME_TYPE, ORDER_DATA, SIGNATURE);

        verify(jsonWriter, times(1)).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
    }

    @Test
    public void logFailedTransactionShouldLogAFailedExternalTransactionWhenUserCancelledIsFalse() throws IOException {
        underTest.logFailure(request, response, PURCHASE_ID, FAILURE_MESSAGE, USER_CANCELLED_FALSE);

        verify(androidInAppBillingService).logFailedTransaction(PURCHASE_ID, FAILURE_MESSAGE);
    }

    @Test
    public void logFailedTransactionShouldLogAFailedExternalTransactionWhenUserCancelledIsNull() throws IOException {
        underTest.logFailure(request, response, PURCHASE_ID, FAILURE_MESSAGE, USER_CANCELLED_NULL);

        verify(androidInAppBillingService).logFailedTransaction(PURCHASE_ID, FAILURE_MESSAGE);
    }

    @Test
    public void logFailedTransactionShouldLogAFailedExternalTransactionWhenUserCancelledIsNotABooleanValue() throws IOException {
        underTest.logFailure(request, response, PURCHASE_ID, FAILURE_MESSAGE, USER_CANCELLED_NOT_TRUE);

        verify(androidInAppBillingService).logFailedTransaction(PURCHASE_ID, FAILURE_MESSAGE);
    }

    @Test
    public void logFailedTransactionShouldLogACancelledExternalTransactionWhenUserCancelledIsTrue() throws IOException {
        underTest.logFailure(request, response, PURCHASE_ID, FAILURE_MESSAGE, USER_CANCELLED_TRUE);

        verify(androidInAppBillingService).logUserCancelledTransaction(PURCHASE_ID, FAILURE_MESSAGE);
    }

    @Test
    public void requestProductsShouldReturnBadRequestIfGameTypeIsMissing() throws IOException {
        underTest.requestProducts(request, response, null);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_BAD_REQUEST));
    }

    @Test
    public void requestProductsShouldReturnUnauthorizedWhenNoSession() throws IOException {
        underTest.requestProducts(request, response, GAME_TYPE);

        verify(jsonWriter, times(1)).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
    }

    @Test
    public void requestProductsShouldShouldWriteAvailableProductsToResponse() throws IOException {
        setUpActiveSessionForPlayer(PLAYER_ID);
        AndroidStoreProducts expectedProducts = setUpExpectedProducts();
        when(androidPromotionService.getAvailableProducts(PLAYER_ID, Platform.ANDROID, GAME_TYPE)).thenReturn(expectedProducts);

        underTest.requestProducts(request, response, GAME_TYPE);

        verify(jsonWriter, times(1)).writeOk(response, expectedProducts);
    }

    private AndroidStoreProducts setUpExpectedProducts() {
        AndroidStoreProducts products = new AndroidStoreProducts();
        products.setPromoId(67687L);
        AndroidStoreProduct productWithPromo = new AndroidStoreProduct("productWithPromo 1", BigDecimal.valueOf(150000));
        productWithPromo.setPromoChips(BigDecimal.valueOf(300000));
        products.addProduct(productWithPromo);

        products.addProduct(new AndroidStoreProduct("product 2 no promotion package", BigDecimal.valueOf(500000)));
        return products;
    }

    private void setUpActiveSessionForPlayer(BigDecimal playerId) {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(
                new LobbySession(BigDecimal.valueOf(3141592), playerId, "", "", partnerId, "", "", null, false, Platform.ANDROID, AuthProvider.YAZINO));
    }
}
