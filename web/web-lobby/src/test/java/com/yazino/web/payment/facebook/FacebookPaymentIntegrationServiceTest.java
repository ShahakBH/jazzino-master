package com.yazino.web.payment.facebook;

import com.restfb.DefaultFacebookClient;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletServiceException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FacebookPaymentIntegrationServiceTest {
    private static final String PAYMENT_ID = "316488408481860";
    private FacebookPaymentIntegrationService underTest;
    private DefaultFacebookClient mockFacebookClient;

    @Before
    public void setUp() throws Exception {
        final YazinoConfiguration yazinoConfig = mock(YazinoConfiguration.class);
        mockFacebookClient = mock(DefaultFacebookClient.class);
        underTest = new FacebookPaymentIntegrationService(yazinoConfig) {
            @Override
            protected DefaultFacebookClient getDefaultFacebookClient(final String token) {
                return mockFacebookClient;
            }
        };
    }

    @Test
    public void serviceShouldLoadDataFromFacebook() throws WalletServiceException {
        String testJson = "{\"id\":\"316488408481860\"," +
                "\"user\":{\"name\":\"Ivan Sanchez\",\"id\":\"787689858\"}," +
                "\"application\":{\"name\":\"isanchez-canvas\",\"namespace\":\"isanchez-canvas\",\"id\":\"311109889034388\"}," +
                "\"actions\":[{\"type\":\"charge\",\"status\":\"completed\",\"currency\":\"USD\",\"amount\":\"25.00\",\"time_created\":\"2013-08-13T10:13:58+0000\",\"time_updated\":\"2013-08-13T10:13:58+0000\"}]," +
                "\"refundable_amount\":{\"currency\":\"USD\",\"amount\":\"25.00\"}," +
                "\"items\":[{" +
                "\"type\":\"IN_APP_PURCHASE\",\"product\":\"http:\\/\\/env-proxy.london.yazino.com\\/isanchez-centos\\/fbog\\/product\\/usd3_25_buys_50k\",\"quantity\":1}]," +
                "\"country\":\"GB\"," +
                "\"request_id\":\"76db59f6-b41a-4e5e-b59a-65a5478a780e\"," +
                "\"created_time\":\"2013-08-13T10:13:58+0000\"," +
                "\"test\":1,\"payout_foreign_exchange_rate\":1}";
        when(mockFacebookClient.fetchObject(PAYMENT_ID, String.class)).thenReturn(testJson);
        final FacebookPayment payment = underTest.retrievePayment("HIGH_STEKS", PAYMENT_ID);
        assertThat(payment.getPromoId(), is(equalTo(null)));
        assertThat(payment.getFacebookUserId(), is(equalTo("787689858")));
        assertThat(payment.getCurrencyCode(), is(equalTo("USD")));
        assertThat(payment.getAmount(), is(comparesEqualTo(BigDecimal.valueOf(25.00))));
        assertThat(payment.getProductId(), is(equalTo("optionUSD3")));//is the webId
        assertThat(payment.getRequestId(), is(equalTo("76db59f6-b41a-4e5e-b59a-65a5478a780e")));
        assertThat(payment.getStatus(), is(equalTo(FacebookPayment.Status.completed)));
        assertThat(payment.getType(), is(equalTo(FacebookPayment.Type.charge)));
        assertThat(payment.getDisputeReason(), is(nullValue()));
        assertThat(payment.getDisputeDate(), is(nullValue()));
    }

    @Test
    public void serviceShouldParseDisputeData() throws WalletServiceException {
        String testJson = "{\"id\":\"316488408481860\"," +
                "\"user\":{\"name\":\"Ivan Sanchez\",\"id\":\"787689858\"}," +
                "\"application\":{\"name\":\"isanchez-canvas\",\"namespace\":\"isanchez-canvas\",\"id\":\"311109889034388\"}," +
                "\"actions\":[{\"type\":\"charge\",\"status\":\"completed\",\"currency\":\"USD\",\"amount\":\"25.00\",\"time_created\":\"2013-08-13T10:13:58+0000\",\"time_updated\":\"2013-08-13T10:13:58+0000\"}]," +
                "\"refundable_amount\":{\"currency\":\"USD\",\"amount\":\"25.00\"}," +
                "\"items\":[{" +
                "\"type\":\"IN_APP_PURCHASE\",\"product\":\"http:\\/\\/env-proxy.london.yazino.com\\/isanchez-centos\\/fbog\\/product\\/usd3_25_buys_50k\",\"quantity\":1}]," +
                "\"country\":\"GB\"," +
                "\"request_id\":\"76db59f6-b41a-4e5e-b59a-65a5478a780e\"," +
                "\"created_time\":\"2013-08-13T10:13:58+0000\"," +
                "\"test\":1,\"payout_foreign_exchange_rate\":1,\n" +
                "  \"disputes\": [\n" +
                "    {\n" +
                "      \"user_comment\": \"I didn't get my Friend Smash coin! Please help!\", \n" +
                "      \"time_created\": \"2014-02-12T01:37:27+0000\", \n" +
                "      \"user_email\": \"email@domain.com\", \n" +
                "      \"status\": \"pending\", \n" +
                "      \"reason\": \"pending\"\n" +
                "    }\n" +
                "  ]}";
        when(mockFacebookClient.fetchObject(PAYMENT_ID, String.class)).thenReturn(testJson);
        final FacebookPayment payment = underTest.retrievePayment("HIGH_STEKS", PAYMENT_ID);
        assertThat(payment.getPromoId(), is(equalTo(null)));
        assertThat(payment.getFacebookUserId(), is(equalTo("787689858")));
        assertThat(payment.getCurrencyCode(), is(equalTo("USD")));
        assertThat(payment.getAmount(), is(comparesEqualTo(BigDecimal.valueOf(25.00))));
        assertThat(payment.getProductId(), is(equalTo("optionUSD3")));//is the webId
        assertThat(payment.getRequestId(), is(equalTo("76db59f6-b41a-4e5e-b59a-65a5478a780e")));
        assertThat(payment.getStatus(), is(equalTo(FacebookPayment.Status.completed)));
        assertThat(payment.getType(), is(equalTo(FacebookPayment.Type.charge)));
        assertThat(payment.getDisputeReason(), is(equalTo("I didn't get my Friend Smash coin! Please help!")));
        assertThat(payment.getDisputeDate(), is(equalTo(new DateTime(2014, 2, 12, 1, 37, 27))));
    }

    @Test
    public void chargebackShouldCreateChargebackPayment() throws WalletServiceException {
        String testJson = "{\"id\":\"316488408481860\"," +
                "\"user\":{\"name\":\"Ivan Sanchez\",\"id\":\"787689858\"}," +
                "\"application\":{\"name\":\"isanchez-canvas\",\"namespace\":\"isanchez-canvas\",\"id\":\"311109889034388\"}," +
                "\"actions\":[{\"type\":\"chargeback\",\"status\":\"completed\",\"currency\":\"USD\",\"amount\":\"25.00\",\"time_created\":\"2013-08-13T10:13:58+0000\",\"time_updated\":\"2013-08-13T10:13:58+0000\"}]," +
                "\"refundable_amount\":{\"currency\":\"USD\",\"amount\":\"25.00\"}," +
                "\"items\":[{" +
                "\"type\":\"IN_APP_PURCHASE\",\"product\":\"http:\\/\\/env-proxy.london.yazino.com\\/isanchez-centos\\/fbog\\/product\\/usd3_25_buys_50k\",\"quantity\":1}]," +
                "\"country\":\"GB\"," +
                "\"request_id\":\"76db59f6-b41a-4e5e-b59a-65a5478a780e\"," +
                "\"created_time\":\"2013-08-13T10:13:58+0000\"," +
                "\"test\":1,\"payout_foreign_exchange_rate\":1}";
        when(mockFacebookClient.fetchObject(PAYMENT_ID, String.class)).thenReturn(testJson);
        final FacebookPayment payment = underTest.retrievePayment("HIGH_STEKS", PAYMENT_ID);
        assertThat(payment.getPromoId(), is(equalTo(null)));
        assertThat(payment.getFacebookUserId(), is(equalTo("787689858")));
        assertThat(payment.getCurrencyCode(), is(equalTo("USD")));
        assertThat(payment.getAmount(), is(comparesEqualTo(BigDecimal.valueOf(25.00))));
        assertThat(payment.getProductId(), is(equalTo("optionUSD3")));//is the webId
        assertThat(payment.getRequestId(), is(equalTo("76db59f6-b41a-4e5e-b59a-65a5478a780e")));
        assertThat(payment.getStatus(), is(equalTo(FacebookPayment.Status.completed)));
        assertThat(payment.getType(), is(equalTo(FacebookPayment.Type.chargeback)));
    }

    @Test
    public void chargebackShouldWorkWithChargebackRequest() throws WalletServiceException {
        String json = "{\n"
                + "   \"id\": \"3603105474213890\",\n"
                + "   \"user\": {\n"
                + "      \"name\": \"Daniel Schultz\",\n"
                + "      \"id\": \"221159\"\n"
                + "   },\n"
                + "   \"application\": {\n"
                + "      \"name\": \"Friend Smash\",\n"
                + "      \"namespace\": \"friendsmashsample\",\n"
                + "      \"id\": \"241431489326925\"\n"
                + "   },\n"
                + "   \"actions\": [\n"
                + "      {\n"
                + "         \"type\": \"charge\",\n"
                + "         \"status\": \"completed\",\n"
                + "         \"currency\": \"USD\",\n"
                + "         \"amount\": \"0.99\",\n"
                + "         \"time_created\": \"2013-03-22T21:18:54+0000\",\n"
                + "         \"time_updated\": \"2013-03-22T21:18:55+0000\"\n"
                + "      },\n"
                + "      {\n"
                + "         \"type\": \"chargeback\",\n"
                + "         \"status\": \"completed\",\n"
                + "         \"currency\": \"USD\",\n"
                + "         \"amount\": \"0.99\",\n"
                + "         \"time_created\": \"2013-05-20T21:18:54+0000\",\n"
                + "         \"time_updated\": \"2013-05-20T21:18:55+0000\"\n"
                + "      }\n"
                + "   ],\n"
                + "   \"refundable_amount\": {\n"
                + "      \"currency\": \"USD\",\n"
                + "      \"amount\": \"0.00\"\n"
                + "   },\n"
                + "   \"items\": [\n"
                + "      {\n"
                + "         \"type\": \"IN_APP_PURCHASE\",\n"
                + "         \"product\": \"http:\\/\\/env-proxy.london.yazino.com\\/isanchez-centos\\/fbog\\/product\\/usd3_25_buys_50k\",\n"
                + "         \"quantity\": 1\n"
                + "      }\n"
                + "   ],\n"
                + "   \"country\": \"US\",\n"
                + "   \"created_time\": \"2013-03-22T21:18:54+0000\",\n"
                + "   \"payout_foreign_exchange_rate\": 1\n"
                + "}";
        when(mockFacebookClient.fetchObject(PAYMENT_ID, String.class)).thenReturn(json);
        final FacebookPayment payment = underTest.retrievePayment("HIGH_STEKS", PAYMENT_ID);
        assertThat(payment.getPromoId(), is(equalTo(null)));
        assertThat(payment.getFacebookUserId(), is(equalTo("221159")));
        assertThat(payment.getCurrencyCode(), is(equalTo("USD")));
        assertThat(payment.getAmount(), is(comparesEqualTo(BigDecimal.valueOf(0.99))));
        assertThat(payment.getProductId(), is(equalTo("optionUSD3")));//is the webId
//        assertThat(payment.getRequestId(), is(equalTo("76db59f6-b41a-4e5e-b59a-65a5478a780e")));
        assertThat(payment.getStatus(), is(equalTo(FacebookPayment.Status.completed)));
        assertThat(payment.getType(), is(equalTo(FacebookPayment.Type.chargeback)));

    }

    @Test
    public void failedPaymentShouldReturnFailedPayment() throws WalletServiceException {
        String failureJson = "{\"id\":\"318520041611433\"," +
                "\"user\":{\"name\":\"Jae Rae\",\"id\":\"100003140322249\"}," +
                "\"application\":{\"name\":\"Rimsin High Stakes\",\"namespace\":\"rimsinhighstakes\",\"id\":\"435242453188353\"}," +
                "\"actions\":[{\"type\":\"charge\",\"status\":\"failed\",\"currency\":\"USD\",\"amount\":\"25.00\",\"time_created\":\"2013-08-22T10:22:02+0000\",\"time_updated\":\"2013-08-22T10:22:04+0000\"}]," +
                "\"refundable_amount\":{\"currency\":\"USD\",\"amount\":\"0.00\"}," +
                "\"items\":[{\"type\":\"IN_APP_PURCHASE\",\"product\":\"http:\\/\\/env-proxy.london.yazino.com\\/jrae-centos\\/fbog\\/product\\/usd3_25_buys_100k_7\",\"quantity\":1}]," +
                "\"country\":\"GB\"," +
                "\"request_id\":\"3a542bcd-6e5c-4064-8ee3-c054982beb29\"," +
                "\"created_time\":\"2013-08-22T10:22:02+0000\"," +
                "\"test\":1,\"payout_foreign_exchange_rate\":1}";
        when(mockFacebookClient.fetchObject(PAYMENT_ID, String.class)).thenReturn(failureJson);
        final FacebookPayment payment = underTest.retrievePayment("HIGH_STEKS", PAYMENT_ID);
        assertThat(payment.getPromoId(), is(equalTo(7l)));
        assertThat(payment.getFacebookUserId(), is(equalTo("100003140322249")));
        assertThat(payment.getCurrencyCode(), is(equalTo("USD")));
        assertThat(payment.getAmount(), is(comparesEqualTo(BigDecimal.valueOf(25.00))));
        assertThat(payment.getProductId(), is(equalTo("optionUSD3")));//is the webId
        assertThat(payment.getRequestId(), is(equalTo("3a542bcd-6e5c-4064-8ee3-c054982beb29")));
        assertThat(payment.getStatus(), is(equalTo(FacebookPayment.Status.failed)));
        assertThat(payment.getType(), is(equalTo(FacebookPayment.Type.charge)));
    }
}
