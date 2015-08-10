package com.yazino.web.payment.amazon;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AmazonInAppBillingRequestValidatorTest {
    public static final String USER_ID = "user-id";
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "product-id";
    private static final String ORDER_ID = "order-data";
    private static final String PURCHASE_TOKEN = "purchase-token";
    private static final String MESSAGE = "message";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private AmazonInAppBillingRequestValidator underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new AmazonInAppBillingRequestValidator();
    }

    @Test
    public void shouldValidateCreditChipsMethodParameters() {
        assertTrue(underTest.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCreditChipsMethodParametersWhenNoUserId() {
        assertFalse(underTest.isValidReceiptRequest(request, response, null, ORDER_ID, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCreditChipsMethodParametersWhenNoOrderId() {
        assertFalse(underTest.isValidReceiptRequest(request, response, USER_ID, null, PRODUCT_ID, GAME_TYPE, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCreditChipsMethodParametersWhenNoProductId() {
        assertFalse(underTest.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, null, GAME_TYPE, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCreditChipsMethodParametersWhenNoGameType() {
        assertFalse(underTest.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, null, PURCHASE_TOKEN));
    }

    @Test
    public void shouldFailCreditChipsMethodParametersWhenNoPurchaseToken() {
        assertFalse(underTest.isValidReceiptRequest(request, response, USER_ID, ORDER_ID, PRODUCT_ID, GAME_TYPE, null));
    }

    @Test
    public void shouldValidateLogFailureMethodParameters() {
        assertTrue(underTest.isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, MESSAGE));
    }

    @Test
    public void shouldFailLogFailureMethodWhenNoOrderId() {
        assertFalse(underTest.isValidLogFailureRequest(request, response, null, GAME_TYPE, PRODUCT_ID, MESSAGE));
    }

    @Test
    public void shouldFailLogFailureMethodWhenNoGameType() {
        assertFalse(underTest.isValidLogFailureRequest(request, response, ORDER_ID, null, PRODUCT_ID, MESSAGE));
    }

    @Test
    public void shouldFailLogFailureMethodWhenNoProductId() {
        assertFalse(underTest.isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, null, MESSAGE));
    }

    @Test
    public void shouldFailLogFailureMethodWhenNoMessage() {
        assertFalse(underTest.isValidLogFailureRequest(request, response, ORDER_ID, GAME_TYPE, PRODUCT_ID, null));
    }
}
