package com.yazino.payment.worldpay.fx;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class CompanyExchangeRates implements Serializable {
    private static final long serialVersionUID = 331847920500244513L;

    private final Set<ExchangeRate> exchangeRates = new HashSet<ExchangeRate>();

    private final long companyNumber;
    private final DateTime processingDate;
    private final DateTime agreementDate;

    public CompanyExchangeRates(final long companyNumber,
                                final DateTime processingDate,
                                final DateTime agreementDate,
                                final Set<ExchangeRate> exchangeRates) {
        notNull(processingDate, "processingDate may not be null");
        notNull(agreementDate, "agreementDate may not be null");

        this.companyNumber = companyNumber;
        this.processingDate = processingDate;
        this.agreementDate = agreementDate;

        if (exchangeRates != null) {
            this.exchangeRates.addAll(exchangeRates);
        }
    }

    public Optional<String> baseCurrency() {
        for (ExchangeRate exchangeRate : exchangeRates) {
            if (exchangeRate.getRate().compareTo(BigDecimal.ONE) == 0) {
                return Optional.fromNullable(exchangeRate.getCurrencyCode());
            }
        }
        return Optional.absent();
    }

    public Optional<ExchangeRate> exchangeRateFor(final String currencyCode) {
        for (ExchangeRate exchangeRate : exchangeRates) {
            if (exchangeRate.getCurrencyCode().equals(currencyCode)) {
                return Optional.fromNullable(exchangeRate);
            }
        }
        return Optional.absent();
    }

    public Set<ExchangeRate> getExchangeRates() {
        return Collections.unmodifiableSet(exchangeRates);
    }

    public long getCompanyNumber() {
        return companyNumber;
    }

    public DateTime getProcessingDate() {
        return processingDate;
    }

    public DateTime getAgreementDate() {
        return agreementDate;
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
        CompanyExchangeRates rhs = (CompanyExchangeRates) obj;
        return new EqualsBuilder()
                .append(this.exchangeRates, rhs.exchangeRates)
                .append(this.companyNumber, rhs.companyNumber)
                .append(this.processingDate, rhs.processingDate)
                .append(this.agreementDate, rhs.agreementDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(exchangeRates)
                .append(companyNumber)
                .append(processingDate)
                .append(agreementDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("exchangeRates", exchangeRates)
                .append("companyNumber", companyNumber)
                .append("processingDate", processingDate)
                .append("agreementDate", agreementDate)
                .toString();
    }
}
