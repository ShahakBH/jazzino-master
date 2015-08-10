package com.yazino.payment.worldpay;

import com.yazino.payment.worldpay.nvp.*;
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
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.yazino.payment.worldpay.MessageCode.NO_CARDS_FOUND;
import static com.yazino.payment.worldpay.MessageCode.forCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class STLinkExternalIntegrationTest {

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
    public void aPaymentTrustSaleMessageSucceeds() {
        final NVPMessage saleMessage = new PaymentTrustSaleMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse response = stLink.send(saleMessage);

        assertThat(response.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(response.get("MessageCode").get()), is(equalTo(MessageCode.APPROVED)));
        assertThat(MessageCode.isSuccessful(response.get("MessageCode")), is(true));
        assertThat(response.get("Message").isPresent(), is(true));
        assertThat(response.get("Message").get(), is(equalTo("Transaction Approved")));
    }

    @Test
    public void paymentTrustAuthorisationWithPastExpiryDateReturnsInvalidCardExpiration() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "092012")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(equalTo(MessageCode.INVALID_CARD_EXPIRATION)));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(false));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));
    }

    @Test
    public void paymentTrustAuthorisationWithDeclinedTestCardReturnsNotAuthorised() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "6766770009991000")
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(equalTo(MessageCode.NOT_AUTHORISED)));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(false));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));
    }

    @Test
    public void paymentTrustAuthorisationWithInsufficientBalanceTestCardReturnsOverLimit() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "4552997502391742")
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(equalTo(MessageCode.OVER_CREDIT_LIMIT)));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(false));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));
    }

    @Test
    public void riskGuardianRequestWithWrongFormatReturnsAWrongFormatError() {
        final NVPMessage rgMessage = new RiskGuardianMessage()
                .withValue("IsTest", 1)
                .withValue("OrderNumber", "WP Internal Test1") // triggers error 400
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "092012")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "1");

        final NVPResponse authResponse = stLink.send(rgMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(equalTo(MessageCode.WRONG_FORMAT_BASIC)));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aMessageWithAnInvalidTransactionTypesThrowsAnIllegalArgumentException() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("TransactionType", "XT")
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "092012")
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        stLink.send(authMessage);
    }

    @Test
    public void paymentTrustAuthorisationAndDepositMessagesSucceed() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(anyOf(equalTo(MessageCode.APPROVED), equalTo(MessageCode.PENDING))));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(true));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));

        final NVPMessage depositMessage = new PaymentTrustDepositMessage()
                .withValue("IsTest", 1)
                .withValue("CurrencyId", authResponse.get("CurrencyId").get())
                .withValue("Amount", authResponse.get("Amount").get())
                .withValue("PTTID", authResponse.get("PTTID").get())
                .withValue("OrderNumber", authResponse.get("OrderNumber").get());

        final NVPResponse depositResponse = stLink.send(depositMessage);

        assertThat(depositResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(depositResponse.get("MessageCode").get()), is(anyOf(equalTo(MessageCode.APPROVED), equalTo(MessageCode.PENDING))));
        assertThat(MessageCode.isSuccessful(depositResponse.get("MessageCode")), is(true));
        assertThat(depositResponse.get("Message").isPresent(), is(true));
        assertThat(depositResponse.get("Message").get(), is(anyOf(equalTo("Transaction Approved"), equalTo("Request pending"))));
    }

    @Test
    public void paymentTrustAuthorisationAndCancellationMessagesSucceed() {
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("AcctNumber", "4200000000000000")
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(anyOf(equalTo(MessageCode.APPROVED), equalTo(MessageCode.PENDING))));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(true));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));

        final NVPMessage depositMessage = new PaymentTrustCancellationMessage()
                .withValue("IsTest", 1)
                .withValue("PTTID", authResponse.get("PTTID").get())
                .withValue("OrderNumber", authResponse.get("OrderNumber").get());

        final NVPResponse depositResponse = stLink.send(depositMessage);

        assertThat(depositResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(depositResponse.get("MessageCode").get()), is(equalTo(MessageCode.CANCELLED_SUCCESSFULLY_BY_REQUEST)));
        assertThat(MessageCode.isSuccessful(depositResponse.get("MessageCode")), is(true));
        assertThat(depositResponse.get("Message").isPresent(), is(true));
        assertThat(depositResponse.get("Message").get(), is(equalTo("Transaction cancelled successfully")));
    }

    @Test
    public void aRiskGuardianMessageSucceeds() {
        final NVPMessage rgMessage = new RiskGuardianMessage()
                .withValue("IsTest", 1)
                .withValue("OrderNumber", "anInternalTxId")
                .withValue("AcctName", "John Smith")
                .withValue("AcctNumber", "4459510002561039")
                .withValue("Email", "j.smith@gmail.com")
                .withValue("ExpDate", "092010")
                .withValue("CurrencyId", "840")
                .withValue("Amount", "56.78")
                .withValue("FirstName", "John")
                .withValue("LastName", "Smith")
                .withValue("Address1", "2130 Gold")
                .withValue("City", "New York")
                .withValue("StateCode", "NY")
                .withValue("CountryCode", "US")
                .withValue("PhoneNumber", "6188565656")
                .withValue("REMOTE_ADDR", "205.188.146.23");

        final NVPResponse response = stLink.send(rgMessage);

        assertThat(response.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(response.get("MessageCode").get()), is(equalTo(MessageCode.OKAY)));
        assertThat(MessageCode.isSuccessful(response.get("MessageCode")), is(true));
        assertThat(response.get("tScore").isPresent(), is(true));
        assertThat(response.get("tScore").get(), is(equalTo("100.0000")));
        assertThat(response.get("tRisk").isPresent(), is(true));
        assertThat(response.get("tRisk").get(), allOf(is(greaterThan("50")), is(lessThan("80"))));
    }

    @Test
    public void requestToAddCardShouldGenerateAOneTimeToken() {
        final NVPMessage message = new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "A")
                .withValue("OTTResultURL", "https://www.yourcompany.com/");

        final NVPResponse response = stLink.send(message);

        assertThat(response.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(response.get("MessageCode").get()), is(equalTo(MessageCode.REQUEST_PENDING)));
        assertThat(MessageCode.isSuccessful(response.get("MessageCode")), is(true));
        assertThat(response.get("Message").isPresent(), is(true));
        assertThat(response.get("CustomerId").isPresent(), is(true));
        assertThat(response.get("OTTProcessURL").isPresent(), is(true));
        assertThat(response.get("RequestType").get(), is(equalTo("G")));
        assertThat(response.get("RDID").isPresent(), is(true));
        assertThat(response.get("OTT").isPresent(), is(true));
    }

    @Test
    public void requestToUpdateCardShouldGenerateOneTimeToken() {
        final NVPMessage message = new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "U")
                .withValue("OTTResultURL", "https://www.yourcompany.com/");

        final NVPResponse response = stLink.send(message);

        assertThat(response.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(response.get("MessageCode").get()), is(equalTo(MessageCode.REQUEST_PENDING)));
        assertThat(MessageCode.isSuccessful(response.get("MessageCode")), is(true));
        assertThat(response.get("Message").isPresent(), is(true));
        assertThat(response.get("CustomerId").isPresent(), is(true));
        assertThat(response.get("OTTProcessURL").isPresent(), is(true));
        assertThat(response.get("RequestType").get(), is(equalTo("G")));
        assertThat(response.get("RDID").isPresent(), is(true));
        assertThat(response.get("OTT").isPresent(), is(true));
    }

    @Test
    public void requestForAllUserCardsShouldFindNoCards() {
        final NVPMessage generateMessage = new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "A")
                .withValue("OTTResultURL", "https://www.bla.com/");
        final NVPResponse generateResponse = stLink.send(generateMessage);

        final String customerId = generateResponse.get("CustomerId").get();
        final NVPMessage queryMessage = new RedirectQueryMessage()
                .withValue("IsTest", 1)
                .withValue("CustomerId", customerId);
        final NVPXmlResponse queryResponse = stLink.expectXMLSend(queryMessage);

        assertThat(queryResponse.getProperty("CustomerId").getValue(), is(equalTo(customerId)));
        assertThat(queryResponse.getProperty("MessageCode").getValue(), is(equalTo("7300")));
        assertThat(queryResponse.getProperty("MerchantId").getValue(), is(equalTo("200161")));
        assertThat(queryResponse.getProperty("TransactionType").getValue(), is(equalTo("RD")));
        assertThat(queryResponse.getProperty("OrderNumber").getValue(), not(isEmptyOrNullString()));
        assertThat(queryResponse.getProperty("RequestType").getValue(), is(equalTo("Q")));
        assertThat(queryResponse.getProperty("Message").getValue(), is(equalTo(NO_CARDS_FOUND.getDescription())));
    }

    @Test
    public void aRiskGuardianMessageSucceedsWithACardId() throws IOException {
        final String customerId = "123456";
        final String cardId = registerCard(customerId);
        final NVPMessage rgMessage = new RiskGuardianMessage()
                .withValue("IsTest", 1)
                .withValue("OrderNumber", "anInternalTxId")
                .withValue("AcctName", "John")
                .withValue("CardId", cardId)
                .withValue("CustomerId", customerId)
                .withValue("Email", "j.smith@gmail.com")
                .withValue("CurrencyId", "840")
                .withValue("Amount", "10.00")
                .withValue("FirstName", "John")
                .withValue("LastName", "Smith")
                .withValue("Address1", "2130 Gold")
                .withValue("City", "New York")
                .withValue("REMOTE_ADDR", "205.188.146.23");

        final NVPResponse response = stLink.send(rgMessage);

        assertThat(response.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(response.get("MessageCode").get()), is(equalTo(MessageCode.OKAY)));
        assertThat(MessageCode.isSuccessful(response.get("MessageCode")), is(true));
        assertThat(response.get("tScore").isPresent(), is(true));
        assertThat(response.get("tScore").get(), is(equalTo("100.0000")));
        assertThat(response.get("tRisk").isPresent(), is(true));
        assertThat(response.get("tRisk").get(), allOf(is(greaterThan("50")), is(lessThan("80"))));
    }

    @Test
    public void paymentTrustAuthorisationWithCardIdMessagesSucceed() throws IOException {
        String playerId = "998888";
        String cardId = registerCard(playerId);
        final NVPMessage authMessage = new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("CardId", cardId)
                .withValue("CurrencyId", 826)
                .withValue("CustomerId", playerId)
                .withValue("Amount", "56.78");

        final NVPResponse authResponse = stLink.send(authMessage);

        assertThat(authResponse.get("MessageCode").isPresent(), is(true));
        assertThat(forCode(authResponse.get("MessageCode").get()), is(anyOf(equalTo(MessageCode.APPROVED), equalTo(MessageCode.PENDING))));
        assertThat(MessageCode.isSuccessful(authResponse.get("MessageCode")), is(true));
        assertThat(authResponse.get("PTTID").isPresent(), is(true));
        assertThat(authResponse.get("OrderNumber").isPresent(), is(true));
    }


    private String registerCard(String playerId) throws IOException {
        final NVPResponse response = stLink.send(new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "A")
                .withValue("CustomerId", playerId)
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
        httpClient.execute(httpPost);
        final NVPResponse nvpResponse = stLink.send(new RedirectQueryOTTMessage()
                .withValue("IsTest", 1)
                .withValue("OTT", saveCardOTT));
        return nvpResponse.get("CardId").get();
    }
}
