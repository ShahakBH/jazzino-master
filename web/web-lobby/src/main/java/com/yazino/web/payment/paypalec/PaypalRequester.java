package com.yazino.web.payment.paypalec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import urn.ebay.api.PayPalAPI.*;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringEscapeUtils.escapeXml;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Component
public class PaypalRequester {
    private static final Logger LOG = LoggerFactory.getLogger(PaypalRequester.class);

    private final PayPalAPIInterfaceServiceService payPalService;

    @Autowired
    public PaypalRequester(@Qualifier("paypalEcCashierConfig") final CashierConfig cashierConfig,
                           @Value("${strata.server.lobby.paypal.config-file}") final String configFile) {
        notNull(cashierConfig, "cashierConfig may not be null");
        notBlank(configFile, "configFile may not be null/bank");

        payPalService = initialisePayPalService(configFile);
    }

    private PayPalAPIInterfaceServiceService initialisePayPalService(final String configFile) {
        try {
            if (new File(configFile).exists()) {
                LOG.debug("Initialising PayPal API from " + configFile);
                return new PayPalAPIInterfaceServiceService(new File(configFile));
            } else {
                LOG.debug("Initialising PayPal API classpath");
                return new PayPalAPIInterfaceServiceService(getClass().getResourceAsStream("/paypal_ec_sdk_config.properties"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialise PayPal SDK (configFile = " + configFile + ")", e);
        }
    }

    public ExpressCheckoutDetails getExpressCheckoutDetails(final String token)
            throws PaypalRequestException {
        final GetExpressCheckoutDetailsReq request = new GetExpressCheckoutDetailsReq();
        request.setGetExpressCheckoutDetailsRequest(new GetExpressCheckoutDetailsRequestType(token));
        try {
            final GetExpressCheckoutDetailsResponseType responseType = payPalService.getExpressCheckoutDetails(request);
            if (isSuccessful(responseType.getAck())) {
                return new ExpressCheckoutDetails(responseType);
            }
            throw new PaypalRequestException(String.format("GetExpressCheckoutDetails return failure code %s; request is %s",
                    responseType.getAck(), request));

        } catch (Exception e) {
            throw new PaypalRequestException("ExpressCheckoutDetails request failed for request " + request, e);
        }
    }

    public String setExpressCheckout(final String returnURL,
                                     final String cancelURL,
                                     final String internalTransactionId,
                                     final String purchaseAmount,
                                     final String purchaseCurrency,
                                     final BigDecimal numberOfChips)
            throws PaypalRequestException {
        final SetExpressCheckoutRequestDetailsType requestDetails = new SetExpressCheckoutRequestDetailsType();
        requestDetails.setReturnURL(escapeXml(returnURL));
        requestDetails.setCancelURL(escapeXml(cancelURL));
        requestDetails.setPaymentAction(PaymentActionCodeType.SALE);
        requestDetails.setPaymentDetails(asList(paymentDetailsFor(internalTransactionId, purchaseAmount, purchaseCurrency, numberOfChips)));
        requestDetails.setReqConfirmShipping("0");
        requestDetails.setNoShipping("1");
        requestDetails.setAllowNote("0");
        final SetExpressCheckoutReq request = new SetExpressCheckoutReq();
        request.setSetExpressCheckoutRequest(new SetExpressCheckoutRequestType(requestDetails));

        try {
            final SetExpressCheckoutResponseType responseType = payPalService.setExpressCheckout(request);
            if (isSuccessful(responseType.getAck())) {
                return responseType.getToken();
            }
            throw new PaypalRequestException(String.format("SetExpressCheckout returned failure code %s; request is %s",
                    responseType.getAck(), request));

        } catch (Exception e) {
            throw new PaypalRequestException("SetExpressCheckout request failed for request " + request, e);
        }
    }

    public ExpressCheckoutPayment doExpressCheckoutPayment(final String token,
                                                           final String payerId,
                                                           final String internalTransactionId,
                                                           final String purchaseAmount,
                                                           final String purchaseCurrency,
                                                           final BigDecimal numberOfChips)
            throws PaypalRequestException {

        final PaymentDetailsType paymentDetails = paymentDetailsFor(internalTransactionId, purchaseAmount, purchaseCurrency, numberOfChips);
        final DoExpressCheckoutPaymentReq request = new DoExpressCheckoutPaymentReq();
        final DoExpressCheckoutPaymentRequestDetailsType requestDetails = new DoExpressCheckoutPaymentRequestDetailsType();
        requestDetails.setToken(token);
        requestDetails.setPayerID(payerId);
        requestDetails.setPaymentAction(PaymentActionCodeType.SALE);
        requestDetails.setPaymentDetails(asList(paymentDetails));
        request.setDoExpressCheckoutPaymentRequest(new DoExpressCheckoutPaymentRequestType(requestDetails));

        try {
            return new ExpressCheckoutPayment(payPalService.doExpressCheckoutPayment(request));

        } catch (Exception e) {
            throw new PaypalRequestException("DoExpressCheckoutPayment request failed for request " + request, e);
        }
    }

    private BasicAmountType amountFor(final String monetaryAmount, final String currencyCode) {
        return new BasicAmountType(CurrencyCodeType.valueOf(currencyCode), monetaryAmount);
    }

    private PaymentDetailsType paymentDetailsFor(final String internalTransactionId,
                                                 final String amount,
                                                 final String currencyCode, final BigDecimal numberOfChips) {
        final PaymentDetailsType paymentDetails = new PaymentDetailsType();
        paymentDetails.setOrderTotal(amountFor(amount, currencyCode));
        paymentDetails.setItemTotal(amountFor(amount, currencyCode));
        paymentDetails.setPaymentDetailsItem(asList(paymentDetailsItemFor(amount, currencyCode, numberOfChips)));
        paymentDetails.setInvoiceID(internalTransactionId);
        return paymentDetails;
    }

    private PaymentDetailsItemType paymentDetailsItemFor(final String amount,
                                                         final String currencyCode,
                                                         final BigDecimal numberOfChips) {
        final PaymentDetailsItemType item = new PaymentDetailsItemType();
        item.setAmount(amountFor(amount, currencyCode));
        item.setQuantity(1);
        item.setItemCategory(ItemCategoryType.DIGITAL);
        item.setName(String.format("%,d Yazino Chips", numberOfChips.longValue()));
        return item;
    }

    private boolean isSuccessful(final AckCodeType ackCodeType) {
        return ackCodeType != null
                && (ackCodeType == AckCodeType.SUCCESS || ackCodeType == AckCodeType.SUCCESSWITHWARNING);
    }
}
