package com.yazino.web.payment.googlecheckout;

import com.google.checkout.sdk.commands.EnvironmentInterface;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.HttpURLConnection;

import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.ORDER_PROCESSING;
import static com.google.checkout.sdk.commands.EnvironmentInterface.CommandType.REPORTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class GoogleCheckoutContextTest {

    public static final String READ_TIME_OUT = "20000";
    public static final String CONNECT_TIMEOUT = "10000";

    @Test
    public void shouldCreateContextforSandboxEnvironment() {
        GoogleCheckoutContext checkoutContext = new GoogleCheckoutContext("SANDBOX", "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);
        // get a url and check it starts with sandbox
        final EnvironmentInterface environment = checkoutContext.getApiContext().getEnvironment();
        String reportsUrl = environment.getUrl(REPORTS, "id");
        assertTrue(reportsUrl.startsWith("https://sandbox.google.com"));
    }

    @Test
    public void shouldCreateContextforProductionEnvironment() {
        GoogleCheckoutContext checkoutContext = new GoogleCheckoutContext("PRODUCTION", "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);
        // get a url and check it starts with sandbox
        final EnvironmentInterface environment = checkoutContext.getApiContext().getEnvironment();
        String reportsUrl = environment.getUrl(ORDER_PROCESSING, "id");
        assertTrue(reportsUrl.startsWith("https://checkout.google.com"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailToCreateContextForUnknownEnvironment() {
        new GoogleCheckoutContext("UNKNOWN", "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailToCreateContextForNullEnvironment() {
        new GoogleCheckoutContext(null, "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);
    }

    @Test
    public void shouldSetReadTimeout() {
        GoogleCheckoutContext checkoutContext = new GoogleCheckoutContext("SANDBOX", "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);

        final HttpURLConnection httpURLConnection = checkoutContext.getApiContext().makeConnection(
                "http://someurl.somewhere");
        assertThat(httpURLConnection.getReadTimeout(), CoreMatchers.is(Integer.parseInt(READ_TIME_OUT)));
    }

    @Test
    public void shouldSetConnectTimeout() {
        GoogleCheckoutContext checkoutContext = new GoogleCheckoutContext("SANDBOX", "id", "key", "currency",
                READ_TIME_OUT, CONNECT_TIMEOUT);

        final HttpURLConnection httpURLConnection = checkoutContext.getApiContext().makeConnection(
                "http://someurl.somewhere");
        assertThat(httpURLConnection.getConnectTimeout(), CoreMatchers.is(Integer.parseInt(CONNECT_TIMEOUT)));
    }

}
