package com.yazino.web.payment.paypalec;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import urn.ebay.api.PayPalAPI.GetExpressCheckoutDetailsResponseType;
import urn.ebay.apis.CoreComponentTypes.BasicAmountType;
import urn.ebay.apis.eBLBaseComponents.GetExpressCheckoutDetailsResponseDetailsType;

import static org.apache.commons.lang3.Validate.notNull;

public class ExpressCheckoutDetails {
    private static final Logger LOG = LoggerFactory.getLogger(ExpressCheckoutDetails.class);

    /**
     * @see "http://stackoverflow.com/questions/16630463/paypal-integration-getexpresscheckout"
     */
    public enum CheckoutStatus {
        PAYMENT_ACTION_NOT_INITIATED("PaymentActionNotInitiated", false),
        PAYMENT_ACTION_FAILED("PaymentActionFailed", true),
        PAYMENT_ACTION_IN_PROGRESS("PaymentActionInProgress", true),
        PAYMENT_COMPLETED("PaymentCompleted", true),
        UNKNOWN("Unknown", true);

        private final String id;
        private final boolean completed;

        private CheckoutStatus(final String id, final boolean completed) {
            this.id = id;
            this.completed = completed;
        }

        public boolean isCompleted() {
            return completed;
        }

        public static CheckoutStatus forId(final String id) {
            for (CheckoutStatus checkoutStatus : values()) {
                if (checkoutStatus.id.equals(id)) {
                    return checkoutStatus;
                }
            }
            return UNKNOWN;
        }
    }

    private final String token;
    private final String payerId;
    private final String amount;
    private final String currency;
    private final String invoiceId;
    private final String transactionId;
    private final CheckoutStatus checkoutStatus;

    public ExpressCheckoutDetails(final GetExpressCheckoutDetailsResponseType checkoutResult) {
        notNull(checkoutResult, "checkoutResult may not be null");

        final GetExpressCheckoutDetailsResponseDetailsType checkoutDetailsResult = checkoutResult.getGetExpressCheckoutDetailsResponseDetails();
        if (checkoutDetailsResult == null) {
            throw new IllegalArgumentException("Malformed result: no response details: " + checkoutResult);
        }

        token = checkoutDetailsResult.getToken();
        checkoutStatus = CheckoutStatus.forId(checkoutDetailsResult.getCheckoutStatus());

        if (checkoutDetailsResult.getPayerInfo() == null) {
            throw new IllegalArgumentException("Malformed result: no payer info: " + checkoutDetailsResult);
        }
        payerId = checkoutDetailsResult.getPayerInfo().getPayerID();

        if (checkoutDetailsResult.getPaymentDetails() == null || checkoutDetailsResult.getPaymentDetails().isEmpty()) {
            throw new IllegalArgumentException("Malformed result: no payment details: " + checkoutDetailsResult);
        } else if (checkoutDetailsResult.getPaymentDetails().size() > 1) {
            LOG.warn("Multiple payment details returned, only expected one: " + checkoutDetailsResult);
        }

        invoiceId = checkoutDetailsResult.getPaymentDetails().get(0).getInvoiceID();
        transactionId = checkoutDetailsResult.getPaymentDetails().get(0).getTransactionId();

        final BasicAmountType orderTotal = checkoutDetailsResult.getPaymentDetails().get(0).getOrderTotal();
        if (orderTotal == null) {
            throw new IllegalArgumentException("Malformed result: no order total on first payment detail: "
                    + checkoutDetailsResult);
        }

        amount = orderTotal.getValue();
        currency = orderTotal.getCurrencyID().getValue();
    }

    public String getToken() {
        return token;
    }

    public String getPayerId() {
        return payerId;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public CheckoutStatus getCheckoutStatus() {
        return checkoutStatus;
    }

    /**
     * The transaction ID for completed transactions only.
     * <p/>
     * This will only be present for completed transactions. Check {@link #getCheckoutStatus()} before using.
     *
     * @return the transaction ID or null if the transaction is incomplete.
     */
    public String getTransactionId() {
        return transactionId;
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
        final ExpressCheckoutDetails rhs = (ExpressCheckoutDetails) obj;
        return new EqualsBuilder()
                .append(token, rhs.token)
                .append(payerId, rhs.payerId)
                .append(amount, rhs.amount)
                .append(currency, rhs.currency)
                .append(invoiceId, rhs.invoiceId)
                .append(transactionId, rhs.transactionId)
                .append(checkoutStatus, rhs.checkoutStatus)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(token)
                .append(payerId)
                .append(amount)
                .append(currency)
                .append(invoiceId)
                .append(checkoutStatus)
                .append(transactionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(token)
                .append(payerId)
                .append(amount)
                .append(currency)
                .append(invoiceId)
                .append(checkoutStatus)
                .append(transactionId)
                .toString();
    }
}
