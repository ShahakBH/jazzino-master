package com.yazino.web.api.payments;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.creditcard.*;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CreditCardAPIPaymentControllerTest {
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private LogoutHelper logoutHelper;
    @Mock
    private CreditCardService creditCardService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;

    private final JsonHelper json = new JsonHelper();

    private CreditCardAPIPaymentController underTest;

    @Before
    public void setUp() throws IOException {
        underTest = new CreditCardAPIPaymentController(webApiResponses, json, lobbySessionCache, creditCardService, logoutHelper);

        when(request.isSecure()).thenReturn(true);
        when(request.getRemoteAddr()).thenReturn("10.9.8.11");
        when(request.getSession()).thenReturn(session);
        final Cookie[] cookies = new Cookie[0];
        when(request.getCookies()).thenReturn(cookies);
        when(lobbySessionCache.getActiveSession(request)).thenReturn(aLobbySession());
    }

    @Test
    public void anInsecureRequestIsRejectedAsABadRequest() throws IOException {
        reset(request);
        when(request.isSecure()).thenReturn(false);

        underTest.processPayment(request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Request must be secure");
    }

    @Test
    public void aRequestWhereNoSessionIsPresentIsRejectedWithUnauthorised() throws IOException {
        reset(lobbySessionCache);

        underTest.processPayment(request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "No session");
    }

    @Test
    public void anEmptyRequestIsRejectedWithBadRequest() throws IOException {
        underTest.processPayment(request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to deserialise request");
    }

    @Test
    public void aMalformedRequestIsRejectedWithBadRequest() throws IOException {
        setRequestTo("malformed");

        underTest.processPayment(request, response);

        verify(webApiResponses).writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Failed to deserialise request");
    }

    @Test
    public void aValidRequestIsPassedToTheCreditCardService() throws IOException {
        setRequestTo(json.serialize(aValidCreditCardForm().build()));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.APPROVED));

        underTest.processPayment(request, response);

        final CreditCardForm creditCardForm = aValidCreditCardForm().build();
        verify(creditCardService).completePurchase(creditCardForm.toPaymentContext(aLobbySession()),
                creditCardForm.toCreditCardDetails(),
                InetAddress.getByAddress(new byte[]{10, 9, 8, 11}));
    }

    @Test
    public void aValidRequestReturnsAnApprovedResponse() throws IOException {
        setRequestTo(json.serialize(aValidCreditCardForm().build()));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.APPROVED));

        underTest.processPayment(request, response);

        verify(webApiResponses).write(response, HttpServletResponse.SC_OK, aJsonResponseFor(PurchaseOutcome.APPROVED));
    }

    @Test
    public void aInvalidRequestReturnsAValidationFailedResponseAndAListOfErrors() throws IOException {
        final CreditCardForm invalidForm = aValidCreditCardForm().withCvc2("3").withCreditCardNumber("4200000000000001").build();
        setRequestTo(json.serialize(invalidForm));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.APPROVED));

        underTest.processPayment(request, response);

        final Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("outcome", PurchaseOutcome.VALIDATION_ERROR.name());
        jsonResponse.put("errors", asList("Invalid Credit Card Number, Security Code entered."));
        verify(webApiResponses).write(response, HttpServletResponse.SC_OK, jsonResponse);
    }

    @Test
    public void aDeclinedRequestReturnsAnDeclinedResponse() throws IOException {
        setRequestTo(json.serialize(aValidCreditCardForm().build()));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.DECLINED));

        underTest.processPayment(request, response);

        verify(webApiResponses).write(response, HttpServletResponse.SC_OK, aJsonResponseFor(PurchaseOutcome.DECLINED));
    }

    @Test
    public void aBlockedRequestReturnsAnBlockedResponse() throws IOException {
        setRequestTo(json.serialize(aValidCreditCardForm().build()));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.PLAYER_BLOCKED));

        underTest.processPayment(request, response);

        verify(webApiResponses).write(response, HttpServletResponse.SC_OK, aJsonResponseFor(PurchaseOutcome.PLAYER_BLOCKED));
    }

    @Test
    public void aBlockedRequestLogsTheUserOut() throws IOException {
        setRequestTo(json.serialize(aValidCreditCardForm().build()));
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(InetAddress.class)))
                .thenReturn(aPurchaseResponseFor(PurchaseOutcome.PLAYER_BLOCKED));

        underTest.processPayment(request, response);

        verify(logoutHelper).logout(session, request, response);
    }

    private Map<String, String> aJsonResponseFor(final PurchaseOutcome outcome) {
        final HashMap<String, String> jsonResponse = new HashMap<>();
        jsonResponse.put("outcome", outcome.name());
        jsonResponse.put("internalTransactionId", "anInternalTransactionId");
        jsonResponse.put("externalTransactionId", "anExternalTransactionId");
        jsonResponse.put("chips", "10000");
        return jsonResponse;
    }

    private PurchaseResult aPurchaseResponseFor(final PurchaseOutcome outcome) {
        return new PurchaseResult("aMerchant", outcome, "anEmail@a.host", "aMessage", Currency.getInstance("GBP"),
                new BigDecimal("10.00"), new BigDecimal("10000"), "4200XXXXXXXX0000", "anInternalTransactionId",
                "anExternalTransactionId", "aTrace");
    }

    private void setRequestTo(final String requestBody) throws IOException {
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
    }

    private LobbySession aLobbySession() {
        return new LobbySession(BigDecimal.valueOf(5355107), BigDecimal.valueOf(300), "aPlayerName",
                "aSessionKey", Partner.YAZINO, "aPictureUrl", "anEmailAddress", null, false, Platform.ANDROID, AuthProvider.YAZINO);
    }

    private CreditCardFormBuilder aValidCreditCardForm() {
        return CreditCardFormBuilder.valueOf()
                .withPaymentOptionId("aPaymentOption")
                .withCreditCardNumber("4200000000000000")
                .withCvc2("888")
                .withExpirationMonth("03")
                .withExpirationYear("2050")
                .withCardHolderName("aName")
                .withEmailAddress("anEmail@a.host")
                .withTermsAndServiceAgreement("agree")
                .withGameType("aGameType");
    }

}
