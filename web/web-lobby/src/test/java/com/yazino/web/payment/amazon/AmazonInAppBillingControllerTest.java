package com.yazino.web.payment.amazon;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.api.RequestException;
import com.yazino.web.controller.util.RequestValidationHelper;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.WebApiResponses;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AmazonInAppBillingControllerTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final String USER_ID = "user-id";
    public static final Long PROMO_ID = 987L;
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "product-id";
    private static final String ORDER_ID = "order-data";
    private static final String PURCHASE_TOKEN = "purchase-token";
    private static final String MESSAGE = "message";
    private static final String CURRENCY = "USD";
    private static final BigDecimal CHIPS = BigDecimal.TEN;
    private static final BigDecimal PRICE = BigDecimal.ONE;
    private static final String PLAYER_NAME = "jack";
    private static final String EMAIL_ADDRESS = "email@here.com";
    public static final String PROMOTION_ID = "1234";
    private static final BigDecimal SESSION_ID = BigDecimal.TEN;

    @Mock
    private HttpServletRequest request;
    @Mock
    InAppBillingService amazonInAppBillingService;
    @Mock
    ReceiptVerification amazonReceiptVerificationService;
    @Mock
    private YazinoPaymentState yazinoPaymentState;
    @Mock
    private AmazonInAppBillingRequestValidator amazonInAppBillingRequestValidator;
    @Mock
    WebApiResponses responseWriter;
    @Mock
    RequestValidationHelper requestValidationHelper;

    private MockHttpServletResponse response;
    private AmazonInAppBillingController underTest;
    private static final Partner partnerId = Partner.YAZINO;

    @Before
    public void init() throws RequestException {
        MockitoAnnotations.initMocks(this);
        underTest = new AmazonInAppBillingController(
                amazonInAppBillingService,
                amazonInAppBillingRequestValidator,
                requestValidationHelper,
                responseWriter);

        response = new MockHttpServletResponse();
        final ChipBundle chipBundle = new ChipBundle();
        chipBundle.setProductId(PRODUCT_ID);
        chipBundle.setPrice(PRICE);
        chipBundle.setChips(CHIPS);
        chipBundle.setCurrency(Currency.getInstance("USD"));
        when(requestValidationHelper.verifySession(request)).thenReturn(new LobbySession(SESSION_ID, PLAYER_ID, PLAYER_NAME, "sessionkey", Partner.YAZINO, "pictureUrl", EMAIL_ADDRESS, null, true, Platform.AMAZON, AuthProvider.FACEBOOK));
    }

    @Test
    public void shouldValidatePurchaseRequest() throws IOException {
        underTest.creditPurchase(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN, PROMOTION_ID);

        verify(amazonInAppBillingRequestValidator)
                .isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN);
    }

    @Test
    public void requestToCreditPurchaseShouldReturnBadRequestWhenRequestValidatorReturnsFalse() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidLogFailureRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        underTest.creditPurchase(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN, PROMOTION_ID);
        verify(responseWriter).writeError(response, HttpStatus.SC_BAD_REQUEST, "userId, internalId, productId, gameType, externalId are required");
    }

    @Test
    public void shouldCreditPlayer() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN)).thenReturn(true);
        underTest.creditPurchase(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN, PROMOTION_ID);

        verify(amazonInAppBillingService).creditPurchase(paymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);
        verify(responseWriter).writeOk(any(HttpServletResponse.class), any());
    }

    private PaymentContext paymentContext() {
        return new PaymentContext(PLAYER_ID, SESSION_ID, PLAYER_NAME, GAME_TYPE, EMAIL_ADDRESS, null, Long.valueOf(PROMOTION_ID), Partner.YAZINO);
    }

    @Test
    public void shouldValidateLogFailureRequest() throws IOException {
        underTest.logFailure(request, response, ORDER_ID, PRODUCT_ID, GAME_TYPE, MESSAGE, PROMOTION_ID);

        verify(amazonInAppBillingRequestValidator).isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, MESSAGE);
    }

    @Test
    public void shouldLogFailure() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, MESSAGE)).thenReturn(true);
        underTest.logFailure(request, response, ORDER_ID, PRODUCT_ID, GAME_TYPE, MESSAGE, PROMOTION_ID);

        verify(amazonInAppBillingService).logFailedTransaction(paymentContext(), PRODUCT_ID, ORDER_ID, MESSAGE);
        verify(responseWriter).writeNoContent(response, HttpStatus.SC_OK);
    }


    @Test
    public void requestPurchaseShouldReturnBadRequestWhenUserIdIsMissing() throws IOException {
        underTest.creditPurchase(request, response, null, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN, PROMOTION_ID);

        verify(responseWriter).writeError(response, HttpStatus.SC_BAD_REQUEST, "userId, internalId, productId, gameType, externalId are required");
    }

    @Test
    public void requestPurchaseShouldHandleBadRequestWhenPromoIdIsNotALong() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN)).thenReturn(true);
        underTest.creditPurchase(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN, "notALong");

        verify(amazonInAppBillingService).creditPurchase(new PaymentContext(PLAYER_ID, SESSION_ID, PLAYER_NAME, GAME_TYPE, EMAIL_ADDRESS, null, null, partnerId), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);
        verify(responseWriter).writeOk(any(HttpServletResponse.class), any());


    }

    @Test
    public void requestToLogFailureShouldReturnBadRequestWhenRequestValidatorReturnsFalse() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidLogFailureRequest(any(HttpServletRequest.class), any(HttpServletResponse.class), anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        underTest.logFailure(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, null, PROMOTION_ID);

        verify(responseWriter).writeError(response, HttpStatus.SC_BAD_REQUEST, "internalId, gameType, productId, message cannot be blank");
    }

    @Test
    public void requestToLogFailureShouldHandleBadRequestWhenPromoIdIsNotALong() throws IOException {
        when(amazonInAppBillingRequestValidator.isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, MESSAGE)).thenReturn(true);

        underTest.logFailure(request, response, ORDER_ID, PRODUCT_ID, GAME_TYPE, MESSAGE, "notALong");

        verify(amazonInAppBillingService).logFailedTransaction(new PaymentContext(PLAYER_ID, SESSION_ID, PLAYER_NAME, GAME_TYPE, EMAIL_ADDRESS, null, null, partnerId), PRODUCT_ID, ORDER_ID, MESSAGE);

        verify(responseWriter).writeNoContent(response, HttpStatus.SC_OK);

    }

}
