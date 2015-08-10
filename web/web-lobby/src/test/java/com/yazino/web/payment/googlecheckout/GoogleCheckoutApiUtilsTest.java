package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.domain.DeliverOrderRequest;
import com.google.checkout.sdk.domain.NotificationHistoryRequest;
import com.google.checkout.sdk.util.Utils;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.BufferedInputStream;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GoogleCheckoutApiUtilsTest {

    @Test
    public void shouldConstructValidNotificationRequest() throws JAXBException {
        // given order number
        final String orderNumber = "551094253599465";
        NotificationHistoryRequest expected = loadRequest("551094253599465-request.xml");

        // when building the order-change-state/new-order request for the order number
        final NotificationHistoryRequest request = GoogleCheckoutApiUtils.buildNotificationHistoryRequest(
                orderNumber);

        // then request should have correct order number and notification types
        assertThat(request.toString(), is(expected.toString()));
    }

    @Test
    public void shouldConstructValidDeliverOrderRequest() throws JAXBException {
        // given order number
        final String orderNumber = "447457885120905";
        DeliverOrderRequest expected = loadDeliverOrderRequest("447457885120905-DeliverOrderRequest.xml");

        // when building the order-change-state/new-order request for the order number
        final DeliverOrderRequest request = GoogleCheckoutApiUtils.buildDeliverOrderRequest(orderNumber);

        assertThat(request.toString(), is(expected.toString()));
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
}
