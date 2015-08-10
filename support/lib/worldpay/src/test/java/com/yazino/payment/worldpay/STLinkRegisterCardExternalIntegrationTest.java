package com.yazino.payment.worldpay;

import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.RedirectGenerateMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryOTTMessage;
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
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.yazino.payment.worldpay.MessageCode.*;
import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class STLinkRegisterCardExternalIntegrationTest {
    private static final String CUSTOMER_ID = "4B8FFD03-2";
    private static final String CUSTOMER_ID_2 = "4B8FFD03-3";

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
    public void shouldAddCardForUser() throws IOException {
        final NVPMessage generateOneTimeTokenMessage = createGenerateToken(CUSTOMER_ID, "A");
        /* save card 6766770009991000 */
        final NVPResponse firstCardAdditionResponse = registerCard(generateOneTimeTokenMessage, "6766770009991000");
        /* save card 4544320001228342 */
        final NVPResponse secondCardAdditionResponse = registerCard(generateOneTimeTokenMessage, "4544320001228342");

        /* query all cards for the customer with this ID */
        final NVPXmlResponse responseForAllCards = stLink.expectXMLSend(new RedirectQueryMessage()
                .withValue("IsTest", 1)
                .withValue("CustomerId", CUSTOMER_ID));

        final MessageCode messageCode = CUSTOMER_INFORMATION_ADDED;
        /* confirm card addition */
        assertThat(firstCardAdditionResponse.get("AcctNumber").get(), is(equalTo("********1000")));
        assertThat(firstCardAdditionResponse.get("AcctName").get(), is(equalTo("John Smith")));
        assertThat(firstCardAdditionResponse.get("CreditCardType").get(), is(equalTo("MA")));
        assertThat(firstCardAdditionResponse.get("IssueCountry").get(), is(equalTo("ES")));
        assertThat(firstCardAdditionResponse.get("CardIssuer").get(), is(equalTo("SISTEMA 4B S.A.")));
        assertThat(firstCardAdditionResponse.get("MessageCode").get(), is(equalTo(valueOf(messageCode.getCode()))));
        assertThat(firstCardAdditionResponse.get("Message").get(), is(equalTo(CUSTOMER_INFORMATION_ADDED.getDescription())));

        /* confirm second card addition */
        assertThat(secondCardAdditionResponse.get("AcctNumber").get(), is(equalTo("********8342")));
        assertThat(secondCardAdditionResponse.get("AcctName").get(), is(equalTo("John Smith")));
        assertThat(secondCardAdditionResponse.get("CreditCardType").get(), is(equalTo("VD")));
        assertThat(secondCardAdditionResponse.get("IssueCountry").get(), is(equalTo("GB")));
        assertThat(secondCardAdditionResponse.get("CardIssuer").get(), is(equalTo("LLOYDS TSB BANK PLC")));
        assertThat(secondCardAdditionResponse.get("MessageCode").get(), is(equalTo(valueOf(messageCode.getCode()))));
        assertThat(secondCardAdditionResponse.get("Message").get(), is(equalTo(CUSTOMER_INFORMATION_ADDED.getDescription())));

        /* check both cards are returned */
        assertThat(responseForAllCards.getProperty("Card").getNestedPropertyGroup(), hasSize(2));
        for (NVPXmlResponse.Property card : responseForAllCards.getProperty("Card").getNestedPropertyGroup()) {
            assertThat(card.getNestedProperty("CardId").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("AcctNumber").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("CardIssuer").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("CreditCardType").getValue(), not(isEmptyOrNullString()));
            assertThat(card.getNestedProperty("IsDefault").getValue(), not(isEmptyOrNullString()));
        }
    }

    @Test
    public void shouldReturnInvalidExpiry() throws IOException {
        final NVPMessage generateOneTimeTokenMessage = createGenerateToken(CUSTOMER_ID_2, "A");
        String ott = stLink.send(generateOneTimeTokenMessage).get("OTT").get();
        httpClient.execute(createPostForm(createSaveCardFormParameters(ott, "6766770009991000", "1999"), "https://ott9.wpstn.com/test/"));

        NVPResponse response = stLink.send(new RedirectQueryOTTMessage().withValue("IsTest", 1).withValue("OTT", ott));
        MessageCode code = MessageCode.forCode(response.get("MessageCode").get());

        assertThat(code, is(equalTo(INVALID_CARD_EXPIRY_DATE)));
    }

    @Test
    public void shouldReturnInvalidCreditCardNumber() throws IOException {
        final String invalidCreditCardNumber = "6766770009991";
        final NVPMessage generateOneTimeTokenMessage = createGenerateToken(CUSTOMER_ID_2, "A");
        String ott = stLink.send(generateOneTimeTokenMessage).get("OTT").get();
        httpClient.execute(createPostForm(createSaveCardFormParameters(ott, invalidCreditCardNumber, currentYear), "https://ott9.wpstn.com/test/"));

        NVPResponse response = stLink.send(new RedirectQueryOTTMessage().withValue("IsTest", 1).withValue("OTT", ott));
        MessageCode code = MessageCode.forCode(response.get("MessageCode").get());

        assertThat(code, is(equalTo(INVALID_CREDIT_CARD_NUMBER2)));
    }

    @Test
    public void shouldReturnInvalidTransaction() throws IOException {
        NVPResponse response = stLink.send(new RedirectQueryOTTMessage().withValue("IsTest", 1).withValue("OTT", "INVALID_TOKEN"));
        MessageCode code = MessageCode.forCode(response.get("MessageCode").get());

        assertThat(code, is(equalTo(INVALID_TRANSACTION3)));
    }

    private NVPResponse registerCard(final NVPMessage generateOneTimeTokenMessage, final String accountNumber) throws IOException {
        final NVPResponse tokenResponseForSavingFirstCard = stLink.send(generateOneTimeTokenMessage);
        String saveFirstCardOTT = tokenResponseForSavingFirstCard.get("OTT").get();
        httpClient.execute(createPostForm(createSaveCardFormParameters(saveFirstCardOTT, accountNumber, currentYear), "https://ott9.wpstn.com/test/"));
        return stLink.send(new RedirectQueryOTTMessage() // this query completes the addition of card
                .withValue("IsTest", 1)
                .withValue("OTT", saveFirstCardOTT));
    }

    private NVPMessage createGenerateToken(final String customerId, String action) {
        return new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", action)
                .withValue("CustomerId", customerId)
                .withValue("OTTResultURL", "http://www.breakmycasino.com");
    }

    private List<NameValuePair> createSaveCardFormParameters(final String saveCardOTT, final String cardNumber, String expiryYear) {
        final List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("IsTest", "1"));
        parameters.add(new BasicNameValuePair("Action", "Add"));
        parameters.add(new BasicNameValuePair("AcctName", "John Smith"));
        parameters.add(new BasicNameValuePair("AcctNumber", cardNumber));
        parameters.add(new BasicNameValuePair("ExpMonth", "12"));
        parameters.add(new BasicNameValuePair("ExpYear", expiryYear));
        parameters.add(new BasicNameValuePair("OTT", saveCardOTT));
        return parameters;
    }

    private HttpUriRequest createPostForm(final List<NameValuePair> parameters, final String ottProcessURL) {
        final HttpPost httpPost = new HttpPost(ottProcessURL);
        httpPost.setEntity(new UrlEncodedFormEntity(parameters, UTF_8));
        return httpPost;
    }
}
