package com.yazino.web.payment.paypalec;

import org.junit.Test;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.CurrencyCodeType;
import urn.ebay.apis.eBLBaseComponents.GetExpressCheckoutDetailsResponseDetailsType;
import urn.ebay.apis.eBLBaseComponents.PayerInfoType;
import urn.ebay.apis.eBLBaseComponents.PaymentDetailsType;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExpressCheckoutDetailsTest {

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoResponseDetailsArePresent() {
        new ExpressCheckoutDetails(responseWith(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoPayerInfoIsPresent() {
        new ExpressCheckoutDetails(responseWith(
                detailsFor("aToken", null, asList(paymentDetailsFor(amountFor("GBP", "100"))))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoPaymentDetailsArePresent() {
        new ExpressCheckoutDetails(responseWith(
                detailsFor("aToken", payerInfoFor("aPayer"), null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoOrderAmountIsPresent() {
        new ExpressCheckoutDetails(responseWith(
                detailsFor("aToken", payerInfoFor("aPayer"), asList(paymentDetailsFor(null)))));
    }

    @Test
    public void aResponseIsParsedCorrectly() {
        final ExpressCheckoutDetails details = new ExpressCheckoutDetails(responseWith(
                detailsFor("aToken", payerInfoFor("aPayer"), asList(paymentDetailsFor(amountFor("GBP", "100"))))));

        assertThat(details, is(not(nullValue())));
        assertThat(details.getToken(), is(equalTo("aToken")));
        assertThat(details.getPayerId(), is(equalTo("aPayer")));
        assertThat(details.getAmount(), is(equalTo("100")));
        assertThat(details.getCurrency(), is(equalTo("GBP")));
    }

    @Test
    public void aResponseWithMultiplePaymentDetailsIgnoresAllButTheFirst() {
        final ExpressCheckoutDetails details = new ExpressCheckoutDetails(responseWith(detailsFor("aToken", payerInfoFor("aPayer"),
                asList(paymentDetailsFor(amountFor("GBP", "100")), paymentDetailsFor(amountFor("USD", "1000"))))));

        assertThat(details, is(not(nullValue())));
        assertThat(details.getToken(), is(equalTo("aToken")));
        assertThat(details.getPayerId(), is(equalTo("aPayer")));
        assertThat(details.getAmount(), is(equalTo("100")));
        assertThat(details.getCurrency(), is(equalTo("GBP")));
    }

    private GetExpressCheckoutDetailsResponseDetailsType detailsFor(final String token,
                                                                    final PayerInfoType payerInfoType,
                                                                    final List<PaymentDetailsType> paymentDetails) {
        final GetExpressCheckoutDetailsResponseDetailsType detailsType = new GetExpressCheckoutDetailsResponseDetailsType();
        detailsType.setToken(token);
        detailsType.setPayerInfo(payerInfoType);
        detailsType.setPaymentDetails(paymentDetails);
        return detailsType;
    }

    private PayerInfoType payerInfoFor(final String payerId) {
        PayerInfoType payerInfo = new PayerInfoType();
        payerInfo.setPayerID(payerId);
        return payerInfo;
    }

    private BasicAmountType amountFor(final String currency,
                                      final String amount) {
        return new BasicAmountType(CurrencyCodeType.valueOf(currency), amount);
    }

    private PaymentDetailsType paymentDetailsFor(final BasicAmountType amount) {
        final PaymentDetailsType paymentDetailsType = new PaymentDetailsType();
        paymentDetailsType.setOrderTotal(amount);
        return paymentDetailsType;
    }

    private GetExpressCheckoutDetailsResponseType responseWith(final GetExpressCheckoutDetailsResponseDetailsType detailsType) {
        final GetExpressCheckoutDetailsResponseType responseType = new GetExpressCheckoutDetailsResponseType();
        responseType.setGetExpressCheckoutDetailsResponseDetails(detailsType);
        return responseType;
    }

}
