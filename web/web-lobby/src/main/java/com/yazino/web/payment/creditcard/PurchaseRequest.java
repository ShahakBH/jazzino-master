package com.yazino.web.payment.creditcard;

import com.yazino.web.payment.CustomerData;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import com.yazino.bi.payment.PaymentOption;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public final class PurchaseRequest implements Serializable {
    private static final long serialVersionUID = 1417629097157570601L;

    private final CustomerData customerData;
    private final BigDecimal accountId;
    private final PaymentOption paymentOption;
    private final DateTime dateTime;
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final Long promotionId;

    public PurchaseRequest(final CustomerData customerData,
                           final BigDecimal accountId,
                           final PaymentOption paymentOption,
                           final DateTime dateTime,
                           final BigDecimal playerId,
                           final BigDecimal sessionId,
                           final Long promotionId) {
        notNull(customerData, "customerData may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(paymentOption, "paymentOption may not be null");
        notNull(dateTime, "dateTime may not be null");
        notNull(playerId, "playerId may not be null");

        this.customerData = customerData;
        this.accountId = accountId;
        this.paymentOption = paymentOption;
        this.dateTime = dateTime;
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.promotionId = promotionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public CustomerData getCustomerData() {
        return customerData;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }


    public PaymentOption getPaymentOption() {
        return paymentOption;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public Long getPromotionId() {
        return promotionId;
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
        final PurchaseRequest rhs = (PurchaseRequest) obj;
        return new EqualsBuilder()
                .append(accountId, rhs.accountId)
                .append(customerData, rhs.customerData)
                .append(paymentOption, rhs.paymentOption)
                .append(dateTime, rhs.dateTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(accountId)
                .append(customerData)
                .append(paymentOption)
                .append(dateTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(accountId)
                .append(customerData)
                .append(paymentOption)
                .toString();
    }
}
