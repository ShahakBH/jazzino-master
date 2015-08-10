package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class CountryCode implements Comparable, Serializable {

    private static final long serialVersionUID = 7241530510766766625L;

    private String code;
    private String name;

    public CountryCode(final String code, final String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public int compareTo(final Object o) {
        final CountryCode other = (CountryCode) o;
        return this.getName().compareTo(other.getName());
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
        final CountryCode rhs = (CountryCode) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(code, rhs.code)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(code)
                .append(name)
                .toHashCode();
    }
}
