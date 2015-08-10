package com.yazino.bi.operations.campaigns.controller;

import com.yazino.bi.campaign.domain.CampaignSchedule;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.util.Date;

public class CampaignScheduleWithName extends CampaignSchedule {
    private String name;


    public CampaignScheduleWithName() {
        super();
    }

    public CampaignScheduleWithName(final Long id, final String name, final DateTime nextRun, final DateTime endTime, final Long runHours, final Long runMinutes) {
        super(id, nextRun, runHours, runMinutes, endTime);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Date getEndTimeAsDate() {
        if (getEndTime() != null) {
            return getEndTime().toDate();
        }
        return null;
    }

    public void setEndTimeAsDate(Date endTime) {
        if (endTime != null) {
            setEndTime(new DateTime(endTime));
        } else {
            setEndTime(null);
        }
    }

    public Date getNextRunTsAsDate() {
        if (getNextRunTs() != null) {
            return getNextRunTs().toDate();
        }
        return null;
    }

    public void setNextRunTsAsDate(Date nextRunTs) {
        if (nextRunTs != null) {
            setNextRunTs(new DateTime(nextRunTs));
        } else {
            setNextRunTs(null);
        }
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
        CampaignScheduleWithName rhs = (CampaignScheduleWithName) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.name, rhs.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(name)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("name", name)
                .toString();
    }
}
