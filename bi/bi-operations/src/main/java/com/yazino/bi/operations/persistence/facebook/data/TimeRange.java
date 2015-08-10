package com.yazino.bi.operations.persistence.facebook.data;

import com.restfb.Facebook;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Calendar;
import java.util.Date;

/**
 * Time range Facebook representation
 */
public class TimeRange {
    private static final long ONE_SECOND = 1000L;
    private static final long ONE_HOUR = 3600L;
    @Facebook("time_start")
    private Long timeStart;

    @Facebook("time_stop")
    private Long timeStop;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("timeStart", timeStart).append("timeStop", timeStop).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TimeRange)) {
            return false;
        }
        final TimeRange castOther = (TimeRange) other;
        return new EqualsBuilder().append(timeStart, castOther.timeStart).append(timeStop, castOther.timeStop)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(timeStart).append(timeStop).toHashCode();
    }

    public TimeRange(final int yearStart, final int monthStart, final int dayStart, final int yearStop,
                     final int monthStop, final int dayStop) {
        final Calendar cal = Calendar.getInstance();
        cal.set(yearStart, monthStart - 1, dayStart, 0, 0, 0);
        this.timeStart = cal.getTime().getTime() / ONE_SECOND;
        cal.set(yearStop, monthStop - 1, dayStop, 0, 0, 0);
        this.timeStop = cal.getTime().getTime() / ONE_SECOND;
    }

    /**
     * Creates the time range between the given dates
     *
     * @param start Start date
     * @param stop  End date
     */
    public TimeRange(final Date start, final Date stop) {
        this.timeStart = start.getTime() / ONE_SECOND;
        this.timeStop = stop.getTime() / ONE_SECOND;
    }

    public Long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(final Long timeStart) {
        this.timeStart = timeStart;
    }

    public Long getTimeStop() {
        return timeStop;
    }

    public void setTimeStop(final Long timeStop) {
        this.timeStop = timeStop;
    }

    /**
     * Converts the date to the Unix format
     *
     * @param unixTimestamp Unix timestamp
     * @return Date matching
     */
    public static Date getDate(final long unixTimestamp) {
        return new Date(unixTimestamp * ONE_SECOND);
    }

    /**
     * Shifts both the start and the end time by the number of hours indicated
     *
     * @param timeShift Time shift reported
     */
    public void shift(final int timeShift) {
        this.timeStart = timeShift * ONE_HOUR + this.timeStart;
        this.timeStop = timeShift * ONE_HOUR + this.timeStop;
    }

}
