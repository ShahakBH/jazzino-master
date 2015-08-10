package com.yazino.payment.worldpay.fx;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WorldPayExchangeRates implements Serializable {
    private static final long serialVersionUID = -2405833286828939467L;

    private final Set<CompanyExchangeRates> companyExchangeRates = new HashSet<>();

    public WorldPayExchangeRates(final Set<CompanyExchangeRates> companyExchangeRates) {
        if (companyExchangeRates != null) {
            this.companyExchangeRates.addAll(companyExchangeRates);
        }
    }

    public Set<CompanyExchangeRates> getCompanyExchangeRates() {
        return Collections.unmodifiableSet(companyExchangeRates);
    }

    public Optional<CompanyExchangeRates> exchangeRatesFor(final long companyNumber) {
        for (CompanyExchangeRates companyExchangeRate : companyExchangeRates) {
            if (companyExchangeRate.getCompanyNumber() == companyNumber) {
                return Optional.fromNullable(companyExchangeRate);
            }
        }
        return Optional.absent();
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
        WorldPayExchangeRates rhs = (WorldPayExchangeRates) obj;
        return new EqualsBuilder()
                .append(this.companyExchangeRates, rhs.companyExchangeRates)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(companyExchangeRates)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("companyExchangeRates", companyExchangeRates)
                .toString();
    }
}
