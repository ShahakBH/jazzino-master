package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.RedirectGenerateMessage;
import com.yazino.payment.worldpay.nvp.RedirectQueryOTTMessage;
import com.yazino.web.payment.CustomerDataBuilder;
import com.yazino.web.payment.creditcard.PurchaseOutcome;
import com.yazino.web.payment.creditcard.PurchaseRequest;
import com.yazino.web.payment.creditcard.PurchaseResult;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.valueOf;
import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class WorldPayCreditCardPaymentServiceExternalIntegrationTest {
    private static final String VISA_CARD = "4779160330716625";
    private static final String DECLINED_VISA_CARD = "4273420005620267";
    private static final String MASTER_CARD = "5301207010000012";
    private static final String STOLEN_CARD = "5046490009998426";
    private static final String DO_NOT_HONOUR_CARD = "4929428456908006";
    private static final String REFERRED_CARD = "4313088431374833";
    private static final String OVER_LIMIT_CARD = "4552997502391742";
    private static final String INSUFFICIENT_FUNDS_CARD = "6718610009970183";
    private static final BigDecimal ACCOUNT_ID = valueOf(344);
    private static final DateTime DATE_TIME = new DateTime();
    private static final BigDecimal PLAYER_ID = TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(100234L);
    private static final long PROMO_ID = 123l;

    @Autowired
    private WorldPayCreditCardPaymentService underTest;
    @Autowired
    private YazinoConfiguration yazinoConfiguration;
    @Autowired
    private STLink stLink;
    @Autowired
    private HttpClient httpClient;

    /*
       Note that most test cards do not pass RiskGuardian with minimal data, so you'll generally need to disable it. Bless.
     */

    @Test
    public void aTestPurchaseIsRiskRejected() {
        enableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "GBP", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("GBP"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.RISK_FAILED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithInGBPWithAVisaCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "GBP", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("GBP"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithInUSDWithAVisaCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "USD", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("USD"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithInEURWithAVisaCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "EUR", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("EUR"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithInAUDWithAVisaCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "AUD", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("AUD"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithInCADWithAVisaCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "CAD", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("CAD"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseCanBeMadeWithACardId() throws IOException {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(new PurchaseRequest(new CustomerDataBuilder()
                .withAmount(TEN)
                .withCurrency(Currency.getInstance(Currency.getInstance("GBP").getCurrencyCode()))
                .withTransactionCountry("UK")
                .withCustomerIPAddress(testHost())
                .withEmailAddress("aTester@yazino.com")
                .withCardId(registerCard())
                .withGameType("BLACKJACK").build(), PLAYER_ID,
                paymentOption("GBP"), DATE_TIME, PLAYER_ID, SESSION_ID, PROMO_ID));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("GBP"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aPurchaseForACurrencyWithNoConfiguredMerchantCausedASystemFailureError() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "NOK", VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.SYSTEM_FAILURE)));
    }

    @Test
    public void aPurchaseCanBeMadeInGBPWithAMasterCard() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10), "GBP", MASTER_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getMoney(), is(equalTo(valueOf(10))));
        assertThat(purchase.getCurrency(), is(equalTo(Currency.getInstance("GBP"))));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
    }

    @Test
    public void aDeclineIsReturnedAsDeclined() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", DECLINED_VISA_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.DECLINED)));
    }

    @Test
    public void aLostOrStolenCardIsReturnedAsBlocked() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", STOLEN_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.PLAYER_BLOCKED)));
    }

    @Test
    public void aDoNotHonourCardIsReturnedAsDeclined() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", DO_NOT_HONOUR_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.DECLINED)));
    }

    @Test
    public void aReferredCardIsReturnedAsReferred() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", REFERRED_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.REFERRED)));
    }

    @Test
    public void aLimitExceededCardIsReturnedAsExceedsLimit() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", OVER_LIMIT_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.EXCEEDS_TRANSACTION_LIMIT)));
    }

    @Test
    public void anInsufficientFundsCardIsReturnedAsInsufficientFunds() {
        disableRiskGuardian();

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequestFor(valueOf(10417), "GBP", INSUFFICIENT_FUNDS_CARD));

        assertThat(purchase, is(not(nullValue())));
        assertThat(purchase.getOutcome(), is(equalTo(PurchaseOutcome.INSUFFICIENT_FUNDS)));
    }

    private void disableRiskGuardian() {
        yazinoConfiguration.setProperty("payment.worldpay.stlink.riskguardian.enabled", false);
    }

    private void enableRiskGuardian() {
        yazinoConfiguration.clearProperty("payment.worldpay.stlink.riskguardian.enabled");
    }

    private String registerCard() throws IOException {
        final NVPResponse response = stLink.send(new RedirectGenerateMessage()
                .withValue("IsTest", 1)
                .withValue("Action", "A")
                .withValue("CustomerId", PLAYER_ID)
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

    private PurchaseRequest aPurchaseRequestFor(final BigDecimal amount,
                                                final String currencyCode,
                                                final String cardNumber) {
        return new PurchaseRequest(customerData(amount, currencyCode, cardNumber).build(),
                ACCOUNT_ID,
                paymentOption(currencyCode),
                DATE_TIME,
                PLAYER_ID,
                SESSION_ID, PROMO_ID);
    }

    private CustomerDataBuilder customerData(final BigDecimal amount,
                                             final String currencyCode,
                                             final String cardNumber) {
        final int nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1;

        return new CustomerDataBuilder()
                .withAmount(amount)
                .withCurrency(Currency.getInstance(currencyCode))
                .withTransactionCountry("UK")
                .withCreditCardNumber(cardNumber)
                .withCvc2("123")
                .withExpirationMonth("12")
                .withExpirationYear(Integer.toString(nextYear).substring(2, 4))
                .withCardHolderName("test")
                .withCustomerIPAddress(testHost())
                .withEmailAddress("aTester@yazino.com")
                .withGameType("BLACKJACK");
    }

    private PaymentOption paymentOption(final String currencyCode) {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setRealMoneyCurrency(currencyCode);
        paymentOption.setId("test-" + currencyCode);
        paymentOption.setAmountRealMoneyPerPurchase(valueOf(10));
        paymentOption.setNumChipsPerPurchase(valueOf(1000));
        return paymentOption;
    }

    private InetAddress testHost() {
        try {
            return Inet4Address.getByName("151.237.239.34");
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
