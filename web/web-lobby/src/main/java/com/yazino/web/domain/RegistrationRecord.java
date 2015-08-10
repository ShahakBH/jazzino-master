package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;

public class RegistrationRecord implements Serializable {
    private static final long serialVersionUID = -6601694613062245009L;
    private final int amount;
    private final DateTime earliestRegistrationTime;

    public RegistrationRecord(final int amount, final DateTime earliestRegistrationTime) {
        this.amount = amount;
        this.earliestRegistrationTime = earliestRegistrationTime;
    }

    public int getRegistrations() {
        return amount;
    }

    public DateTime getEarliestRegistrationTime() {
        return earliestRegistrationTime;
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
        final RegistrationRecord rhs = (RegistrationRecord) obj;
        return new EqualsBuilder()
                .append(amount, rhs.amount)
                .append(earliestRegistrationTime, rhs.earliestRegistrationTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(amount)
                .append(earliestRegistrationTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(amount)
                .append(earliestRegistrationTime)
                .toString();
    }

}
