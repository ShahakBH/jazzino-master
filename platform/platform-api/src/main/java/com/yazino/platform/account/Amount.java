package com.yazino.platform.account;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class Amount implements Serializable {
    private static final long serialVersionUID = -2030269979905973569L;

    private final Currency currency;
    private final BigDecimal quantity;

    public Amount(final Currency currency, final BigDecimal quantity) {
        notNull(currency, "currency may not be null");
        notNull(quantity, "quantity may not be null");

        this.currency = currency;
        this.quantity = quantity;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getQuantity() {
        return quantity;
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
        final Amount rhs = (Amount) obj;
        return new EqualsBuilder()
                .append(currency, rhs.currency)
                .append(quantity, rhs.quantity)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(currency)
                .append(quantity)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(currency)
                .append(quantity)
                .toString();
    }
}
