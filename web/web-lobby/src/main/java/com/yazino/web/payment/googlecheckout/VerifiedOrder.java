package com.yazino.web.payment.googlecheckout;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.math.BigDecimal;

public class VerifiedOrder implements Serializable {

    private String orderId;
    private OrderStatus status;
    private String productId;
    private String currencyCode;
    private BigDecimal price;
    private BigDecimal chips;
    private transient BigDecimal defaultChips;

    public BigDecimal getChips() {
        return chips;
    }

    public void setChips(final BigDecimal chips) {
        this.chips = chips;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(final OrderStatus status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(final String productId) {
        this.productId = productId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDefaultChips() {
        return defaultChips;
    }

    public void setDefaultChips(BigDecimal defaultChips) {
        this.defaultChips = defaultChips;
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
        final VerifiedOrder rhs = (VerifiedOrder) obj;
        return new EqualsBuilder()
                .append(orderId, rhs.orderId)
                .append(status, rhs.status)
                .append(productId, rhs.productId)
                .append(currencyCode, rhs.currencyCode)
                .append(price, rhs.price)
                .append(chips, rhs.chips)
                .append(defaultChips, rhs.defaultChips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(orderId)
                .append(status)
                .append(productId)
                .append(currencyCode)
                .append(price)
                .append(chips)
                .append(defaultChips)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE)
                .append(orderId)
                .append(status)
                .append(productId)
                .append(currencyCode)
                .append(price)
                .append(chips)
                .append(defaultChips)
                .toString();
    }
}
