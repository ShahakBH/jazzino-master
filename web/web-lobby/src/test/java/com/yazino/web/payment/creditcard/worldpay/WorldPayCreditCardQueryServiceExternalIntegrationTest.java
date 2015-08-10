package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.RedirectGenerateMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryOTTMessage;
import com.yazino.web.domain.payment.RegisteredCardQueryResult;
import com.yazino.web.domain.payment.RegisteredCreditCardDetails;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class WorldPayCreditCardQueryServiceExternalIntegrationTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100000);
    private WorldPayCreditCardQueryService underTest;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;
    @Autowired
    private STLink stLink;
    @Autowired
    private HttpClient httpClient;
    
    @Before
    public void setup() throws Exception {
        initMocks(this);
        underTest = new WorldPayCreditCardQueryService(yazinoConfiguration, stLink);
    }
    
    @Test
    public void shouldRetrieveCardsForPlayerId() throws Exception {
        enableWorldPay();
        registerCardForPlayer(PLAYER_ID);

        RegisteredCardQueryResult result = underTest.retrieveCardsFor(PLAYER_ID);

        assertThat("Player has one registered card", result.getCreditCardDetailList().size(), is(equalTo(1)));
        final RegisteredCreditCardDetails cardDetails = result.getCreditCardDetailList().iterator().next();
        assertThat("Player name is John Smith", cardDetails.getAccountName(), is(equalTo("John Smith")));
    }

    private String registerCardForPlayer(BigDecimal playerId) throws IOException {
        final NVPResponse response = stLink.send(new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "A")
                .withValue("CustomerId", playerId.toString())
                .withValue("OTTResultURL", "http://www.breakmycasino.com"));
        String saveCardOTT = response.get("OTT").get();
        final List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("IsTest", "1"));
        parameters.add(new BasicNameValuePair("Action", "Add"));
        parameters.add(new BasicNameValuePair("AcctName", "John Smith"));
        parameters.add(new BasicNameValuePair("AcctNumber", "4200000000000000"));
        parameters.add(new BasicNameValuePair("ExpMonth", "12"));
        parameters.add(new BasicNameValuePair("ExpYear", Integer.toString(new DateTime().getYear())));
        parameters.add(new BasicNameValuePair("OTT", saveCardOTT));
        final HttpPost httpPost = new HttpPost("https://ott9.wpstn.com/test/");
        httpPost.setEntity(new UrlEncodedFormEntity(parameters, UTF_8));
        httpPost.getParams().setParameter(COOKIE_POLICY, IGNORE_COOKIES);
        httpClient.execute(httpPost);
        final NVPResponse nvpResponse = stLink.send(new RedirectQueryOTTMessage()
                .withValue("IsTest", 1)
                .withValue("OTT", saveCardOTT));
        return nvpResponse.get("CardId").get();
    }

    private void enableWorldPay() {
        yazinoConfiguration.setProperty("payment.worldpay.stlink.enabled", true);
        yazinoConfiguration.setProperty("payment.worldpay.stlink.testmode", true);
    }

}
