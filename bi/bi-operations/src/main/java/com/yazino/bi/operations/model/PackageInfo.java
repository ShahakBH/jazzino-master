package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Information on the buying package
 */
public class PackageInfo {
    private String currency;
    private Long amount;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("currency", currency).append("amount", amount).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PackageInfo)) {
            return false;
        }
        final PackageInfo castOther = (PackageInfo) other;
        return new EqualsBuilder().append(currency, castOther.currency).append(amount, castOther.amount).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(currency).append(amount).toHashCode();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(final Long amount) {
        this.amount = amount;
    }
}
