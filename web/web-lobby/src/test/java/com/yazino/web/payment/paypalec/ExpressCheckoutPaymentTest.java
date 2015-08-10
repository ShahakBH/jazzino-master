package com.yazino.web.payment.paypalec;

import org.junit.Test;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static urn.ebay.apis.eBLBaseComponents.AckCodeType.*;

public class ExpressCheckoutPaymentTest {

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoResponseDetailsArePresent() {
        new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESS, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoPaymentInfoArePresent() {
        new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESS, detailsFor("aToken", null)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void anIllegalArgumentIsThrownWhenNoGrossAmountIsPresent() {
        new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESS,
                detailsFor("aToken", asList(paymentInfoFor("aTransactionId", null)))));
    }

    @Test
    public void aSuccessfulResponseIsParsedCorrectly() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESS,
                detailsFor("aToken", asList(paymentInfoFor("aTransactionId", amountFor("GBP", "100"))))));

        assertThat(payment, is(not(nullValue())));
        assertThat(payment.getToken(), is(equalTo("aToken")));
        assertThat(payment.getTransactionId(), is(equalTo("aTransactionId")));
        assertThat(payment.getTimestamp(), is(equalTo("aTimestamp")));
        assertThat(payment.getAmount(), is(equalTo("100")));
        assertThat(payment.getCurrency(), is(equalTo("GBP")));
        assertThat(payment.isSuccessful(), is(true));
    }

    @Test
    public void aSuccessfulWithWarningResponseIsParsedAsSuccessful() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESSWITHWARNING,
                detailsFor("aToken", asList(paymentInfoFor("aTransactionId", amountFor("GBP", "100"))))));

        assertThat(payment.isSuccessful(), is(true));
    }

    @Test
    public void aResponseWithNoAckIsParsedAsFailure() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith("aTimestamp", null,
                detailsFor("aToken", asList(paymentInfoFor("aTransactionId", amountFor("GBP", "100"))))));

        assertThat(payment.isSuccessful(), is(false));
    }

    @Test
    public void aFailureResponseIsParsedCorrectly() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith("aTimestamp", FAILURE,
                asList(anError("001", "aShortMessage"), anError("002", "anotherShortMessage")),
                detailsFor("aToken", asList(paymentInfoFor("aTransactionId", amountFor("GBP", "100"))))));

        final Map<String, String> expectedErrorMap = new HashMap<String, String>();
        expectedErrorMap.put("001", "aShortMessage");
        expectedErrorMap.put("002", "anotherShortMessage");
        assertThat(payment, is(not(nullValue())));
        assertThat(payment.getToken(), is(equalTo("aToken")));
        assertThat(payment.getTransactionId(), is(equalTo("aTransactionId")));
        assertThat(payment.getTimestamp(), is(equalTo("aTimestamp")));
        assertThat(payment.getAmount(), is(equalTo("100")));
        assertThat(payment.getCurrency(), is(equalTo("GBP")));
        assertThat(payment.getErrors(), is(equalTo(expectedErrorMap)));
        assertThat(payment.isSuccessful(), is(false));
    }

    private ErrorType anError(final String errorCode, final String shortMessage) {
        final ErrorType errorType = new ErrorType();
        errorType.setErrorCode(errorCode);
        errorType.setShortMessage(shortMessage);
        return errorType;
    }

    @Test
    public void aFailureResponseWithNoFurtherInformationIsParsedCorrectly() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith(null, FAILURE, detailsFor(null, null)));

        assertThat(payment, is(not(nullValue())));
        assertThat(payment.getToken(), is(nullValue()));
        assertThat(payment.getTransactionId(), is(nullValue()));
        assertThat(payment.getTimestamp(), is(nullValue()));
        assertThat(payment.getAmount(), is(nullValue()));
        assertThat(payment.getCurrency(), is(nullValue()));
        assertThat(payment.isSuccessful(), is(false));
    }

    @Test
    public void aResponseWithMultiplePaymentInfosIgnoresAllAfterTheFirst() {
        final ExpressCheckoutPayment payment = new ExpressCheckoutPayment(responseWith("aTimestamp", SUCCESS, detailsFor("aToken",
                asList(paymentInfoFor("aTransactionId", amountFor("GBP", "100")), paymentInfoFor("anotherTransactionId", amountFor("AUD", "102"))))));

        assertThat(payment, is(not(nullValue())));
        assertThat(payment.getToken(), is(equalTo("aToken")));
        assertThat(payment.getTransactionId(), is(equalTo("aTransactionId")));
        assertThat(payment.getTimestamp(), is(equalTo("aTimestamp")));
        assertThat(payment.getAmount(), is(equalTo("100")));
        assertThat(payment.getCurrency(), is(equalTo("GBP")));
    }

    private DoExpressCheckoutPaymentResponseDetailsType detailsFor(final String token,
                                                                   final List<PaymentInfoType> paymentInfos) {
        final DoExpressCheckoutPaymentResponseDetailsType detailsType = new DoExpressCheckoutPaymentResponseDetailsType();
        detailsType.setToken(token);
        detailsType.setPaymentInfo(paymentInfos);
        return detailsType;
    }

    private BasicAmountType amountFor(final String currency,
                                      final String amount) {
        return new BasicAmountType(CurrencyCodeType.valueOf(currency), amount);
    }

    private PaymentInfoType paymentInfoFor(final String transactionId, final BasicAmountType amount) {
        final PaymentInfoType paymentInfoType = new PaymentInfoType();
        paymentInfoType.setGrossAmount(amount);
        paymentInfoType.setTransactionID(transactionId);
        return paymentInfoType;
    }

    private DoExpressCheckoutPaymentResponseType responseWith(final String timestamp,
                                                              final AckCodeType ackCodeType,
                                                              final DoExpressCheckoutPaymentResponseDetailsType detailsType) {
        return responseWith(timestamp, ackCodeType, null, detailsType);
    }

    private DoExpressCheckoutPaymentResponseType responseWith(final String timestamp,
                                                              final AckCodeType ackCodeType,
                                                              final List<ErrorType> errors,
                                                              final DoExpressCheckoutPaymentResponseDetailsType detailsType) {
        final DoExpressCheckoutPaymentResponseType responseType = new DoExpressCheckoutPaymentResponseType();
        responseType.setTimestamp(timestamp);
        responseType.setAck(ackCodeType);
        responseType.setDoExpressCheckoutPaymentResponseDetails(detailsType);
        responseType.setErrors(errors);
        return responseType;
    }

}
