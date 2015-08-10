package com.yazino.web.payment.chipbundle;

import java.math.BigDecimal;
import java.util.Currency;

public class ChipBundle {
    private String productId;
    private BigDecimal chips;  // the actual number of chips given, will be the same as default chips if bundle is not promoted
    private BigDecimal defaultChips;
    private BigDecimal price;
    private Currency currency;

    public ChipBundle() {
    }

    public ChipBundle(String productId, BigDecimal chips, BigDecimal defaultChips, BigDecimal price, Currency currency) {
        this.productId = productId;
        this.chips = chips;
        this.defaultChips = defaultChips;
        this.price = price;
        this.currency = currency;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public void setChips(BigDecimal chips) {
        this.chips = chips;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
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
        final ChipBundle rhs = (ChipBundle) obj;
        return new org.apache.commons.lang3.builder.EqualsBuilder()
                .append(productId, rhs.productId)
                .append(price, rhs.price)
                .append(currency, rhs.currency)
                .append(chips, rhs.chips)
                .append(defaultChips, rhs.defaultChips)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new org.apache.commons.lang3.builder.HashCodeBuilder()
                .append(productId)
                .append(price)
                .append(chips)
                .append(defaultChips)
                .append(currency)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
                .append(productId)
                .append(chips)
                .append(defaultChips)
                .append(price)
                .append(currency)
                .toString();
    }

    public void setDefaultChips(BigDecimal defaultChips) {
        this.defaultChips = defaultChips;
    }

    public BigDecimal getDefaultChips() {
        return defaultChips;
    }
}
