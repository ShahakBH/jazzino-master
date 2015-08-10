package com.yazino.payment.worldpay;

import com.yazino.payment.worldpay.nvp.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class STLinkUpdateCardExternalIntegrationTest {
    private static final String CUSTOMER_ID = "4B8FFD03-4";

    @Autowired
    private STLink stLink;
    @Autowired
    private HttpClient httpClient;

    private String currentYear;

    @Before
    public void setUp() {
        currentYear = Integer.toString(new DateTime().getYear());
    }

    @Test
    public void shouldUpdateDefaultCard() throws IOException {
        /* REGISTER TWO CREDIT CARDS WITH CUSTOMER */
        final NVPMessage generateOneTimeTokenMessage = createGenerateToken(CUSTOMER_ID, "A");
        final NVPResponse cardOneRegistrationResponse = registerCard(generateOneTimeTokenMessage, "6766770009991000");
        final NVPResponse cardTwoRegistrationResponse = registerCard(generateOneTimeTokenMessage, "4544320001228342");

        /* CHECK THE DEFAULT IS CARD 2 */
        Map<String, NVPXmlResponse.Property> cards = queryAllCardsForCustomer(CUSTOMER_ID);
        final String firstCardId = cardOneRegistrationResponse.get("CardId").get();
        assertThat(getCardDefaultStatus(firstCardId, cards), is(equalTo("0"))); // first card is not default
        assertThat(getCardDefaultStatus(cardTwoRegistrationResponse.get("CardId").get(), cards), is(equalTo("1"))); // second card is default

        /* UPDATE CARD 1 TO BE THE DEFAULT */
        NVPMessage redirectUpdateMessage = new RedirectUpdateMessage()
                .withValue("CustomerId", CUSTOMER_ID)
                .withValue("CardId", firstCardId)
                .withValue("IsTest", "1")
                .withValue("IsDefault", "1");
        stLink.send(redirectUpdateMessage);

        /* CHECK THE DEFAULT IS CARD 1 */
        cards = queryAllCardsForCustomer(CUSTOMER_ID);
        assertThat(getCardDefaultStatus(firstCardId, cards), is(equalTo("1"))); // first card is now default
        assertThat(getCardDefaultStatus(cardTwoRegistrationResponse.get("CardId").get(), cards), is(equalTo("0"))); // second card is no longer default
    }

    private String getCardDefaultStatus(final String cardId, final Map<String, NVPXmlResponse.Property> cards) {
        return cards.get(cardId).getNestedProperty("IsDefault").getValue();
    }

    private Map<String, NVPXmlResponse.Property> queryAllCardsForCustomer(final String customerId) {
        final NVPXmlResponse responseForAllCards = stLink.expectXMLSend(new RedirectQueryMessage()
                .withValue("IsTest", 1)
                .withValue("CustomerId", customerId));
        Map<String, NVPXmlResponse.Property> cards = new HashMap<>();
        for (NVPXmlResponse.Property card : responseForAllCards.getProperty("Card").getNestedPropertyGroup()) {
            final String cardId = card.getNestedProperty("CardId").getValue();
            cards.put(cardId, card);
        }
        return cards;
    }

    private NVPMessage createGenerateToken(final String customerId, String action) {
        return new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", action)
                .withValue("CustomerId", customerId)
                .withValue("OTTResultURL", "http://www.breakmycasino.com");
    }

    private NVPResponse registerCard(final NVPMessage generateOneTimeTokenMessage, final String accountNumber) throws IOException {
        final NVPResponse tokenResponseForSavingFirstCard = stLink.send(generateOneTimeTokenMessage);
        String saveFirstCardOTT = tokenResponseForSavingFirstCard.get("OTT").get();
        httpClient.execute(postForm(createSaveCardFormParameters(saveFirstCardOTT, accountNumber), "https://ott9.wpstn.com/test/"));
        return stLink.send(new RedirectQueryOTTMessage() // this query completes the addition of card
                .withValue("IsTest", 1)
                .withValue("OTT", saveFirstCardOTT));
    }

    private List<NameValuePair> createSaveCardFormParameters(final String saveCardOTT, final String cardNumber) {
        final List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("IsTest", "1"));
        parameters.add(new BasicNameValuePair("Action", "Add"));
        parameters.add(new BasicNameValuePair("AcctName", "John Smith"));
        parameters.add(new BasicNameValuePair("AcctNumber", cardNumber));
        parameters.add(new BasicNameValuePair("ExpMonth", "12"));
        parameters.add(new BasicNameValuePair("ExpYear", currentYear));
        parameters.add(new BasicNameValuePair("OTT", saveCardOTT));
        return parameters;
    }

    private HttpUriRequest postForm(final List<NameValuePair> parameters, final String ottProcessURL) {
        final HttpPost httpPost = new HttpPost(ottProcessURL);
        httpPost.setEntity(new UrlEncodedFormEntity(parameters, UTF_8));
        return httpPost;
    }

}
