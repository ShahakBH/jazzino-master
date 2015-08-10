package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Represents a period of time during a day.
 */
public class DayPeriod implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DayPeriod.class);

    private static final long serialVersionUID = -6383358549163881197L;
    private static final List<String> DAYS = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private static final String PATTERN
            = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)"
            + "@([0-2][0-9]:[0-5][0-9])\\-([0-2][0-9]:[0-5][0-9])";
    private static final int INDEX_DAY = 1;
    private static final int INDEX_START_TIME = 2;
    private static final int INDEX_END_TIME = 3;

    private int day = DateTimeConstants.MONDAY;
    private LocalTime startTime = new LocalTime(0, 0);
    private LocalTime endTime = new LocalTime(0, 0);
    private DateTimeZone dateTimeZone = DateTimeZone.UTC;

    public DayPeriod() {
    }

    public DayPeriod(final String formatted) {
        final Matcher matcher = Pattern.compile(PATTERN).matcher(formatted);
        if (matcher.matches()) {
            setDay(DAYS.indexOf(matcher.group(INDEX_DAY)) + 1);
            setStartTime(matcher.group(INDEX_START_TIME));
            setEndTime(matcher.group(INDEX_END_TIME));
        } else {
            throw new IllegalArgumentException("Invalid format " + formatted);
        }
    }

    public boolean isWithinPeriod(final DateTime dateTime) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking if [{}] is on day [{}] within period [{}]-[{}]",
                    dateTime, day, startTime, endTime);
        }
        if (dateTime.getDayOfWeek() == day) {
            final LocalTime nowTime = dateTime.toLocalTime();
            final int millisOfDay = nowTime.getMillisOfDay();
            return millisOfDay >= startTime.getMillisOfDay() && millisOfDay < endTime.getMillisOfDay();
        }
        return false;
    }

    public void setDay(final int day) {
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public void setStartTime(final String startTime) {
        notNull(startTime, "startTime must not be null");
        this.startTime = new LocalTime(startTime);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setEndTime(final String endTime) {
        notNull(endTime, "endTime must not be null");
        this.endTime = new LocalTime(endTime);
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setDayName(final String dayName) {
        if (!DAYS.contains(dayName)) {
            throw new IllegalArgumentException("invalid day, values should be " + DAYS);
        }
        setDay(DAYS.indexOf(dayName) + 1);
    }

    public String getDayName() {
        return DAYS.get(day - 1);
    }

    public DateTimeZone getDateTimeZone() {
        return dateTimeZone;
    }

    public void setDateTimeZone(DateTimeZone dateTimeZone) {
        notNull(dateTimeZone, "dateTimeZone must not be null");
        this.dateTimeZone = dateTimeZone;
    }

    public String toFormattedPeriod() {
        final StringBuilder builder = new StringBuilder();
        builder.append(DAYS.get(day - 1));
        builder.append("@");
        builder.append(startTime.toString("HH:mm"));
        builder.append("-");
        builder.append(endTime.toString("HH:mm"));
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(day);
        builder.append(startTime);
        builder.append(endTime);
        return builder.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof DayPeriod)) {
            return false;
        }
        final DayPeriod that = (DayPeriod) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(day, that.day);
        builder.append(startTime, that.startTime);
        builder.append(endTime, that.endTime);
        return builder.isEquals();
    }

    @Override
    public String toString() {
        return toFormattedPeriod();
    }
}
