package com.yazino.platform.reference;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Comparator;

import static org.apache.commons.lang3.Validate.notBlank;

public class Country implements Serializable, Comparable<Country>, Comparator<Country> {
    private static final long serialVersionUID = -2854100816184626331L;

    private final String iso3166CountryCode;
    private final String iso4217CurrencyCode;
    private final String name;

    public Country(final String iso3166CountryCode,
                   final String name,
                   final String iso4217CurrencyCode) {
        notBlank(iso3166CountryCode, "iso3166CountryCode may not be null or blank");
        notBlank(name, "name may not be null or blank");
        notBlank(iso4217CurrencyCode, "iso4217CurrencyCode may not be null or blank");

        this.iso3166CountryCode = iso3166CountryCode;
        this.iso4217CurrencyCode = iso4217CurrencyCode;
        this.name = name;
    }

    public String getIso3166CountryCode() {
        return iso3166CountryCode;
    }

    public String getIso4217CurrencyCode() {
        return iso4217CurrencyCode;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(final Country otherCountry) {
        return compare(this, otherCountry);
    }

    @Override
    public int compare(final Country country1, final Country country2) {
        if (country1 == country2) {
            return 0;
        } else if (country1 == null) {
            return -1;
        } else if (country2 == null) {
            return 1;
        }
        return country1.getName().compareTo(country2.getName());
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
        final Country rhs = (Country) obj;
        return new EqualsBuilder()
                .append(iso3166CountryCode, rhs.iso3166CountryCode)
                .append(name, rhs.name)
                .append(iso4217CurrencyCode, rhs.iso4217CurrencyCode)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(iso3166CountryCode)
                .append(name)
                .append(iso4217CurrencyCode)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(iso3166CountryCode)
                .append(name)
                .append(iso4217CurrencyCode)
                .toString();
    }
}
