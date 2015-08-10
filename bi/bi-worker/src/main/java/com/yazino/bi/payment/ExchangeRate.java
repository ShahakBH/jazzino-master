package com.yazino.bi.payment;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class ExchangeRate implements Serializable {
    private static final long serialVersionUID = -6805559593881023343L;

    private Currency currency;
    private Currency baseCurrency;
    private BigDecimal exchangeRate;
    private DateTime settlementDate;

    public ExchangeRate(final Currency currency,
                        final Currency baseCurrency,
                        final BigDecimal exchangeRate,
                        final DateTime settlementDate) {
        notNull(currency, "currency may not be null");
        notNull(baseCurrency, "baseCurrency may not be null");
        notNull(exchangeRate, "exchangeRate may not be null");
        notNull(settlementDate, "settlementDate may not be null");

        this.currency = currency;
        this.baseCurrency = baseCurrency;
        this.exchangeRate = exchangeRate;
        this.settlementDate = settlementDate;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public DateTime getSettlementDate() {
        return settlementDate;
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
                .append(this.currency, rhs.currency)
                .append(this.baseCurrency, rhs.baseCurrency)
                .append(this.exchangeRate, rhs.exchangeRate)
                .append(this.settlementDate, rhs.settlementDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(currency)
                .append(baseCurrency)
                .append(exchangeRate)
                .append(settlementDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("currency", currency)
                .append("baseCurrency", baseCurrency)
                .append("exchangeRate", exchangeRate)
                .append("settlementDate", settlementDate)
                .toString();
    }
}
