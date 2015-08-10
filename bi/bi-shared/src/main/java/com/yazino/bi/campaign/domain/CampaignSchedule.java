package com.yazino.bi.campaign.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class CampaignSchedule {

    private Long campaignId;
    private DateTime nextRunTs;
    private Long runHours;
    private Long runMinutes;
    private DateTime endTime;

    // required for mvc binding
    public CampaignSchedule() {
    }

    public CampaignSchedule(final Long campaignId, DateTime nextRunTs, final Long runHours, final Long runMinutes, final DateTime endTime) {
        this.campaignId = campaignId;
        this.nextRunTs = nextRunTs;
        this.runHours = runHours;
        this.runMinutes = runMinutes;
        this.endTime = endTime;
    }

    public DateTime calculateNextRunTs() {
        final DateTime dateTime;
        if (runHours == 0L) { // calc time from now
            dateTime = new DateTime(new DateTime().getMillis()
                    + (DateTimeConstants.MILLIS_PER_HOUR * runHours)
                    + DateTimeConstants.MILLIS_PER_MINUTE * runMinutes);
        } else {
            dateTime = new DateTime(nextRunTs.getMillis()
                    + (DateTimeConstants.MILLIS_PER_HOUR * runHours)
                    + DateTimeConstants.MILLIS_PER_MINUTE * runMinutes);
        }
        return dateTime;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public DateTime getNextRunTs() {
        return nextRunTs;
    }

    public Long getRunHours() {
        return runHours;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setCampaignId(final Long campaignId) {
        this.campaignId = campaignId;
    }

    public void setNextRunTs(final DateTime nextRunTs) {
        this.nextRunTs = nextRunTs;
    }

    public void setRunHours(final Long runHours) {
        this.runHours = runHours;
    }

    public void setEndTime(final DateTime endTime) {
        this.endTime = endTime;
    }

    public Long getRunMinutes() {
        return runMinutes;
    }

    public void setRunMinutes(final Long runMinutes) {
        this.runMinutes = runMinutes;
    }

    public boolean isExpired() {
        final DateTime currentDateTime = new DateTime();
        if (endTime == null) {
            return false;
        }
        return (endTime.compareTo(currentDateTime) < 0);
    }

    public boolean isInFuture() {
        final DateTime currentDateTime = new DateTime();
        if (nextRunTs == null || endTime == null) {
            return false;
        }
        return (nextRunTs.compareTo(currentDateTime) > 0);
    }

    public boolean isActive() {
        return (this.isInFuture() && !this.isExpired());
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
        CampaignSchedule rhs = (CampaignSchedule) obj;
        return new EqualsBuilder()
                .append(this.campaignId, rhs.campaignId)
                .append(this.nextRunTs, rhs.nextRunTs)
                .append(this.runHours, rhs.runHours)
                .append(this.runMinutes, rhs.runMinutes)
                .append(this.endTime, rhs.endTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(campaignId)
                .append(nextRunTs)
                .append(runHours)
                .append(runMinutes)
                .append(endTime)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("campaignId", campaignId)
                .append("nextRunTs", nextRunTs)
                .append("runHours", runHours)
                .append("runMinutes", runMinutes)
                .append("endTime", endTime)
                .toString();
    }
}
