package com.yazino.web.payment.facebook;

import com.yazino.platform.account.WalletServiceException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

/**
 * See http://developers.facebook.com/docs/reference/api/payment/
 */
public class FacebookPayment {
    private final FacebookProductUrl product;
    private final Status status;
    private final Type type;
    private final String facebookUserId;
    private final String requestId;
    private final String currencyCode;
    private final BigDecimal amount;
    private final DateTime disputeDate;
    private final String disputeReason;

    enum Type {
        charge, refund, chargeback, chargeback_reversal
    }

    enum Status {
        inited, processing, completed, failed
    }

    public FacebookPayment(final String productId,
                           final Status status,
                           final Type type,
                           final String facebookUserId,
                           final String requestId,
                           final String currencyCode,
                           final BigDecimal amount,
                           final String disputeReason,
                           final DateTime disputeDate)
            throws WalletServiceException {
        this.status = status;
        this.type = type;
        this.facebookUserId = facebookUserId;
        this.requestId = requestId;
        this.currencyCode = currencyCode;
        this.amount = amount;
        this.product = new FacebookProductUrl(productId);
        this.disputeReason = disputeReason;
        this.disputeDate = disputeDate;
    }

    public String getProductId() {
        return product.getWebPackage();
    }

    public Long getPromoId() {
        return product.getPromoId();
    }

    public Status getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFacebookUserId() {
        return facebookUserId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public DateTime getDisputeDate() {
        return disputeDate;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        FacebookPayment rhs = (FacebookPayment) obj;
        return new EqualsBuilder()
                .append(this.product, rhs.product)
                .append(this.status, rhs.status)
                .append(this.type, rhs.type)
                .append(this.facebookUserId, rhs.facebookUserId)
                .append(this.requestId, rhs.requestId)
                .append(this.currencyCode, rhs.currencyCode)
                .append(this.amount, rhs.amount)
                .append(this.disputeReason, rhs.disputeReason)
                .append(this.disputeDate, rhs.disputeDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(product)
                .append(status)
                .append(type)
                .append(facebookUserId)
                .append(requestId)
                .append(currencyCode)
                .append(amount)
                .append(disputeReason)
                .append(disputeDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("product", product)
                .append("status", status)
                .append("type", type)
                .append("facebookUserId", facebookUserId)
                .append("requestId", requestId)
                .append("currencyCode", currencyCode)
                .append("amount", amount)
                .append("disputeReason", disputeReason)
                .append("disputeDate", disputeDate)
                .toString();
    }

}
