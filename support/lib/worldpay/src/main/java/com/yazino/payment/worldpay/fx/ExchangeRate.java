package com.yazino.payment.worldpay.fx;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang.Validate.notNull;

public class ExchangeRate implements Serializable {
    private static final long serialVersionUID = 3945789163890499872L;

    private final String currencyCode;
    private final String currencyName;
    private final BigDecimal rate;

    public ExchangeRate(final String currencyCode, final String currencyName, final BigDecimal rate) {
        notNull(currencyCode, "currencyCode may not be null");
        notNull(currencyName, "currencyName may not be null");
        notNull(rate, "rate may not be null");

        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.rate = rate;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public BigDecimal getRate() {
        return rate;
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
        ExchangeRate rhs = (ExchangeRate) obj;
        return new EqualsBuilder()
                .append(this.currencyCode, rhs.currencyCode)
                .append(this.currencyName, rhs.currencyName)
                .append(this.rate, rhs.rate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(currencyCode)
                .append(currencyName)
                .append(rate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("currencyCode", currencyCode)
                .append("currencyName", currencyName)
                .append("rate", rate)
                .toString();
    }
}
