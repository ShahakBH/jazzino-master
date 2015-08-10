package com.yazino.web.payment.paypalec;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import urn.ebay.api.PayPalAPI.DoExpressCheckoutPaymentResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.AckCodeType;
import urn.ebay.apis.eBLBaseComponents.DoExpressCheckoutPaymentResponseDetailsType;
import urn.ebay.apis.eBLBaseComponents.ErrorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class ExpressCheckoutPayment {
    private static final Logger LOG = LoggerFactory.getLogger(ExpressCheckoutPayment.class);

    private final Map<String, String> errors = new HashMap<String, String>();

    private final boolean successful;
    private final String transactionId;
    private final String timestamp;
    private final String token;
    private final String amount;
    private final String currency;

    public ExpressCheckoutPayment(final DoExpressCheckoutPaymentResponseType checkoutResponse) {
        notNull(checkoutResponse, "checkoutResponse may not be null");

        successful = successFrom(checkoutResponse);
        timestamp = checkoutResponse.getTimestamp();

        DoExpressCheckoutPaymentResponseDetailsType responseDetails = responseDetailsFrom(checkoutResponse, successful);

        token = tokenFrom(responseDetails);
        transactionId = transactionIdFrom(responseDetails, successful);

        final BasicAmountType grossAmount = grossAmountFrom(responseDetails, successful);
        if (grossAmount != null) {
            amount = grossAmount.getValue();
            currency = grossAmount.getCurrencyID().getValue();
        } else {
            amount = null;
            currency = null;
        }

        errors.putAll(errorsFrom(checkoutResponse));
    }

    private Map<String, String> errorsFrom(final DoExpressCheckoutPaymentResponseType checkoutResponse) {
        final Map<String, String> errorCodesToMessages = new HashMap<String, String>();
        if (checkoutResponse.getErrors() != null) {
            for (ErrorType errorType : checkoutResponse.getErrors()) {
                errorCodesToMessages.put(errorType.getErrorCode(), errorType.getShortMessage());
            }
        }
        return errorCodesToMessages;
    }

    private String tokenFrom(final DoExpressCheckoutPaymentResponseDetailsType responseDetails) {
        if (responseDetails != null) {
            return responseDetails.getToken();
        }
        return null;
    }

    private boolean successFrom(final DoExpressCheckoutPaymentResponseType checkoutResponse) {
        final AckCodeType ackCodeType = checkoutResponse.getAck();
        return ackCodeType != null
                && (ackCodeType == AckCodeType.SUCCESS || ackCodeType == AckCodeType.SUCCESSWITHWARNING);
    }

    private DoExpressCheckoutPaymentResponseDetailsType responseDetailsFrom(final DoExpressCheckoutPaymentResponseType checkoutResponse,
                                                                            final boolean throwExceptionIfMissing) {
        DoExpressCheckoutPaymentResponseDetailsType responseDetails = checkoutResponse.getDoExpressCheckoutPaymentResponseDetails();
        if (responseDetails == null && throwExceptionIfMissing) {
            throw new IllegalArgumentException("Malformed result: no response details: " + checkoutResponse);
        }
        return responseDetails;
    }

    private BasicAmountType grossAmountFrom(final DoExpressCheckoutPaymentResponseDetailsType responseDetails,
                                            final boolean throwExceptionIfMissing) {
        if (responseDetails == null || responseDetails.getPaymentInfo() == null || responseDetails.getPaymentInfo().isEmpty()) {
            if (throwExceptionIfMissing) {
                throw new IllegalArgumentException("Malformed result: no payment info: " + responseDetails);
            } else {
                return null;
            }
        }

        final BasicAmountType grossAmount = responseDetails.getPaymentInfo().get(0).getGrossAmount();
        if (grossAmount == null && throwExceptionIfMissing) {
            throw new IllegalArgumentException("Malformed result: no gross amount on first payment info: "
                    + responseDetails);
        }
        return grossAmount;
    }

    private String transactionIdFrom(final DoExpressCheckoutPaymentResponseDetailsType responseDetails,
                                     final boolean throwExceptionIfMissing) {
        if (responseDetails == null || responseDetails.getPaymentInfo() == null || responseDetails.getPaymentInfo().isEmpty()) {
            if (throwExceptionIfMissing) {
                throw new IllegalArgumentException("Malformed result: no payment info: " + responseDetails);
            } else {
                return null;
            }
        }

        if (responseDetails.getPaymentInfo().size() > 1) {
            LOG.warn("Multiple payment infos returned, only expected one: " + responseDetails);
        }

        return responseDetails.getPaymentInfo().get(0).getTransactionID();
    }

    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getToken() {
        return token;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ExpressCheckoutPayment rhs = (ExpressCheckoutPayment) obj;
        return new EqualsBuilder()
                .append(successful, rhs.successful)
                .append(token, rhs.token)
                .append(timestamp, rhs.timestamp)
                .append(transactionId, rhs.transactionId)
                .append(amount, rhs.amount)
                .append(currency, rhs.currency)
                .append(errors, rhs.errors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(successful)
                .append(token)
                .append(timestamp)
                .append(transactionId)
                .append(amount)
                .append(currency)
                .append(errors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(successful)
                .append(token)
                .append(timestamp)
                .append(transactionId)
                .append(amount)
                .append(currency)
                .append(errors)
                .toString();
    }
}
