package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.commands.ApiContext;
import com.google.checkout.sdk.domain.*;
import com.google.checkout.sdk.util.HttpUrlException;
import com.google.checkout.sdk.util.Utils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.ORDER_PROCESSING;
import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.REPORTS;
import static com.yazino.web.payment.googlecheckout.Order.Status.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class GoogleCheckoutApiIntegrationTest {
    @Mock
    private GoogleCheckoutContext googleCheckoutContext;

    @Mock
    private ApiContext apiContext;

    private GoogleCheckoutApiIntegration underTest;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        underTest = new GoogleCheckoutApiIntegration(googleCheckoutContext);
        when(googleCheckoutContext.getApiContext()).thenReturn(apiContext);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWithNullContext() {
        new GoogleCheckoutApiIntegration(null);
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void retrieveOrderStateShouldThrowExceptionWhenOrderNumberIsNull(){
        underTest.retrieveOrderState(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrieveOrderStateShouldThrowExceptionWhenOrderNumberIsEmpty(){
        underTest.retrieveOrderState("");
    }

    @Test
    public void orderStatusIsINVALID_ORDER_NUMBER_whenGettingOrderStateForInvalidOrderNumber()
            throws JAXBException {
        // given an invalid order number
        final String orderNumber = "665657";
        final NotificationHistoryResponse response = new NotificationHistoryResponse();
        NotificationHistoryResponse.InvalidOrderNumbers invalidOrderNumbers =
                new NotificationHistoryResponse.InvalidOrderNumbers();
        invalidOrderNumbers.getGoogleOrderNumber().add(orderNumber);
        response.setInvalidOrderNumbers(invalidOrderNumbers);
        when(apiContext.postCommand(eq(REPORTS), Matchers.any(JAXBElement.class))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        assertThat(order.getStatus(), is(INVALID_ORDER_NUMBER));
    }

    @Test
    public void orderStatusIsINVALID_ORDER_NUMBER_whenGettingOrderStateForMalformedOrderNumber()
            throws JAXBException, MalformedURLException {
        // given a malformed order number (should be integer)
        final String orderNumber = "665657klkl";
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Ids are integral numbers, but you sent: 665657klkl");
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);

        when(apiContext.postCommand(eq(REPORTS), Matchers.any(JAXBElement.class)))
                .thenThrow(new HttpUrlException(new URL("https://aurl"), 400, request, errorResponse, null));

        final Order order = underTest.retrieveOrderState(orderNumber);

        assertThat(order.getStatus(), is(INVALID_ORDER_NUMBER));
    }

    @Test
    public void orderStatusIsERROR_whenCartHas2Items()
            throws JAXBException, MalformedURLException {
        // given the request/response
        final String orderNumber = "551094253599465";
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);
        final NotificationHistoryResponse response = loadResponse("551094253599465-WITH-2-ITEMS-REVIEWING.xml");
        when(apiContext.postCommand(eq(REPORTS), argThat(NotificationHistoryRequestJAXBMatcher.matches(
                request.toJAXB())))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        Order expected = new Order("551094253599465", ERROR);
        assertThat(order, is(expected));
    }

    @Test
    public void orderStatusIsPAYMENT_NOT_AUTHORIZED_whenOrderIsBeingReviewed() throws FileNotFoundException, JAXBException {
        // given the request/response
        final String orderNumber = "551094253599465";
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);
        final NotificationHistoryResponse response = loadResponse("551094253599465-REVIEWING.xml");
        when(apiContext.postCommand(eq(REPORTS), argThat(NotificationHistoryRequestJAXBMatcher.matches(
                request.toJAXB())))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        Order expected = buildExpectedOrderStateForOrder551094253599465(PAYMENT_NOT_AUTHORIZED);
        assertThat(order, is(expected));
    }

    private Order buildExpectedOrderStateForOrder551094253599465(Order.Status withStatus) {
        Order state = new Order("551094253599465", withStatus);
        state.setCurrencyCode("GBP");
        state.setPrice(new BigDecimal("150.00"));
        state.setProductId("POKER_150GBP_1500000_CHIPS");
        return state;
    }

    @Test
    public void orderStatusIsPAYMENT_AUTHORIZED_whenLastestOrderStateIsCHARGEABLE() throws FileNotFoundException, JAXBException {
        // given the request/response
        final String orderNumber = "551094253599465";
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);
        final NotificationHistoryResponse response = loadResponse("551094253599465-CHARGEABLE.xml");
        when(apiContext.postCommand(eq(REPORTS), argThat(NotificationHistoryRequestJAXBMatcher.matches(
                request.toJAXB())))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        Order expected = buildExpectedOrderStateForOrder551094253599465(PAYMENT_AUTHORIZED);
        assertThat(order, is(expected));
    }

    @Test
    public void orderStatusIsPAYMENT_AUTHORIZED_whenLastestOrderStateIsCHARGING() throws FileNotFoundException, JAXBException {
        // given the request/response
        final String orderNumber = "551094253599465";
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);
        final NotificationHistoryResponse response = loadResponse("551094253599465-CHARGING.xml");
        when(apiContext.postCommand(eq(REPORTS), argThat(NotificationHistoryRequestJAXBMatcher.matches(
                request.toJAXB())))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        Order expected = buildExpectedOrderStateForOrder551094253599465(PAYMENT_AUTHORIZED);
        assertThat(order, is(expected));
    }

    @Test
    public void orderStatusIsPAYMENT_AUTHORIZED_whenLastestOrderStateIsCHARGED() throws FileNotFoundException, JAXBException {
        // given the request/response
        final String orderNumber = "551094253599465";
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);
        final NotificationHistoryResponse response = loadResponse("551094253599465-CHARGED.xml");
        when(apiContext.postCommand(eq(REPORTS), argThat(NotificationHistoryRequestJAXBMatcher.matches(
                request.toJAXB())))).thenReturn(response);

        final Order order = underTest.retrieveOrderState(orderNumber);

        Order expected = buildExpectedOrderStateForOrder551094253599465(PAYMENT_AUTHORIZED);
        assertThat(order, is(expected));
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void markOrderAsDeliveredShouldThrowExceptionWhenOrderNumberIsNull(){
        underTest.markOrderAsDelivered(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void markOrderAsDeliveredShouldThrowExceptionWhenOrderNumberIsEmpty(){
        underTest.markOrderAsDelivered("");
    }


    @Test
    public void markOrderAsDelivered_shouldReturnFalse_whenGivenAMalformedOrderNumber()
            throws JAXBException, MalformedURLException {
        final String orderNumber = "x665657klkl";
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrorMessage("Ids are integral numbers, but you sent: 665657klkl");
        final DeliverOrderRequest request = GoogleCheckoutApiUtils.buildDeliverOrderRequest(orderNumber);

        when(apiContext.postCommand(eq(ORDER_PROCESSING), Matchers.any(JAXBElement.class)))
                .thenThrow(new HttpUrlException(new URL("https://aurl"), 400, request, errorResponse, null));

        final boolean delivered = underTest.markOrderAsDelivered(orderNumber);

        assertFalse(delivered);
    }

    @Test
    public void markOrderAsDelivered_shouldReturnTrue_whenGivenAKnownOrderNumber() {
        final String orderNumber = "551094253599465";
        final DeliverOrderRequest request = GoogleCheckoutApiUtils.buildDeliverOrderRequest(orderNumber);
        when(apiContext.postCommand(eq(ORDER_PROCESSING), Matchers.any(JAXBElement.class))).thenReturn(new RequestReceivedResponse());

        final boolean delivered = underTest.markOrderAsDelivered(orderNumber);

        assertTrue(delivered);
    }

    private NotificationHistoryResponse loadResponse(String responseFilename) throws JAXBException {
        final InputStream inputStream = getClass().getResourceAsStream(responseFilename);
        return (NotificationHistoryResponse) Utils.fromXML(new BufferedInputStream(
                inputStream)).getValue();
    }

    private NotificationHistoryRequest loadRequest(String requestFilename) throws JAXBException {
        final InputStream inputStream = getClass().getResourceAsStream(requestFilename);
        return (NotificationHistoryRequest) Utils.fromXML(new BufferedInputStream(
                inputStream)).getValue();
    }

    private DeliverOrderRequest loadDeliverOrderRequest(String requestFilename) throws JAXBException {
        final InputStream inputStream = getClass().getResourceAsStream(requestFilename);
        return (DeliverOrderRequest) Utils.fromXML(new BufferedInputStream(
                inputStream)).getValue();
    }

    // matches on google order number only
    static class NotificationHistoryRequestJAXBMatcher extends TypeSafeMatcher<JAXBElement<NotificationHistoryRequest>> {
        private JAXBElement<NotificationHistoryRequest> expected;

        public NotificationHistoryRequestJAXBMatcher(JAXBElement<NotificationHistoryRequest> expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(JAXBElement<NotificationHistoryRequest> notificationHistoryRequestJAXBElement) {
            return expected.getValue().getOrderNumbers().getGoogleOrderNumber()
                           .equals(notificationHistoryRequestJAXBElement.getValue()
                                                                        .getOrderNumbers()
                                                                        .getGoogleOrderNumber());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expected == null ? "null" : expected.toString());
        }

        public static NotificationHistoryRequestJAXBMatcher matches(JAXBElement<NotificationHistoryRequest> request) {
            return new NotificationHistoryRequestJAXBMatcher(request);
        }
    }

//    static class NotificationHistoryRequestIsEqual extends TypeSafeMatcher<NotificationHistoryRequest> {
//        private NotificationHistoryRequest expected;
//
//        public NotificationHistoryRequestIsEqual(final NotificationHistoryRequest request) {
//            this.expected = request;
//        }
//
//        @Override
//        protected boolean matchesSafely(final NotificationHistoryRequest other) {
//            return expected.getOrderNumbers().getGoogleOrderNumber()
//                           .equals(other.getOrderNumbers().getGoogleOrderNumber())
//                    && expected.getNotificationTypes().getNotificationType()
//                               .equals(other.getNotificationTypes().getNotificationType());
//        }
//
//        @Override
//        public void describeTo(final Description description) {
//            description.appendText(expected == null ? "null" : expected.toString());
//        }
//
//        @Factory
//        public static <T> TypeSafeMatcher<NotificationHistoryRequest> equalTo(NotificationHistoryRequest request) {
//            return new NotificationHistoryRequestIsEqual(request);
//        }
//    }
}
