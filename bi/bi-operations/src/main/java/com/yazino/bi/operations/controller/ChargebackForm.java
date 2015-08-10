package com.yazino.bi.operations.controller;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

public class ChargebackForm implements Serializable {
    private static final long serialVersionUID = -4211447156866098148L;

    private Date startDate;
    private Date endDate;
    private boolean onlyChallengeReasons;

    public ChargebackForm() {
    }

    public ChargebackForm(final Date startDate,
                          final Date endDate,
                          final boolean onlyChallengeReasons) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.onlyChallengeReasons = onlyChallengeReasons;
    }

    public Date getStartDate() {
        if (startDate == null) {
            return new DateTime().minusDays(7).toDate();
        }
        return startDate;
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        if (endDate == null) {
            return new DateTime().toDate();
        }
        return endDate;
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    public boolean isOnlyChallengeReasons() {
        return onlyChallengeReasons;
    }

    public void setOnlyChallengeReasons(final boolean onlyChallengeReasons) {
        this.onlyChallengeReasons = onlyChallengeReasons;
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
        ChargebackForm rhs = (ChargebackForm) obj;
        return new EqualsBuilder()
                .append(this.startDate, rhs.startDate)
                .append(this.endDate, rhs.endDate)
                .append(this.onlyChallengeReasons, rhs.onlyChallengeReasons)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(startDate)
                .append(endDate)
                .append(onlyChallengeReasons)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("startDate", startDate)
                .append("endDate", endDate)
                .append("onlyChallengeReasons", onlyChallengeReasons)
                .toString();
    }
}
