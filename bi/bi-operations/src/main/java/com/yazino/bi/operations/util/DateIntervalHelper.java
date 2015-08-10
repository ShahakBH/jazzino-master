package com.yazino.bi.operations.util;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;

/**
 * Date conversions
 */
public final class DateIntervalHelper {
    /**
     * No public constructor
     */
    private DateIntervalHelper() {
    }

    /**
     * Determines the start date of the range selected by the command
     *
     * @param startMonth Starting year-month parameter
     * @param endMonth   Ending year-month parameter
     * @return Start date
     */
    public static Date getStartDate(final String startMonth, final String endMonth) {
        if (startMonth == null || startMonth.length() < 6) {
            return null;
        }
        final Calendar retval = Calendar.getInstance();
        retval.set(Integer.parseInt(startMonth.substring(0, 4)), Integer.parseInt(startMonth.substring(4)) - 1, 1, 0,
                0, 0);
        retval.set(Calendar.MILLISECOND, 0);
        return retval.getTime();
    }

    /**
     * Determines the end date of the range selected by the command
     *
     * @param startMonth Starting year-month parameter
     * @param endMonth   Ending year-month parameter
     * @return End date
     */
    public static Date getEndDate(final String startMonth, final String endMonth) {
        if (endMonth == null || endMonth.length() < 6) {
            return null;
        }
        final Calendar retval = Calendar.getInstance();
        retval.set(Integer.parseInt(endMonth.substring(0, 4)), Integer.parseInt(endMonth.substring(4)) - 1, 1, 0, 0, 0);
        retval.set(Calendar.MILLISECOND, 0);
        retval.set(Calendar.DAY_OF_MONTH, retval.getActualMaximum(Calendar.DAY_OF_MONTH));
        retval.set(Calendar.HOUR_OF_DAY, retval.getActualMaximum(Calendar.HOUR_OF_DAY));
        retval.set(Calendar.MINUTE, retval.getActualMaximum(Calendar.MINUTE));
        retval.set(Calendar.SECOND, retval.getActualMaximum(Calendar.SECOND));
        retval.set(Calendar.MILLISECOND, retval.getActualMaximum(Calendar.MILLISECOND));
        return retval.getTime();
    }

    /**
     * Returns the first moment of the given date
     *
     * @param date Date to convert
     * @return First moment of the given date
     */
    public static Date getDateStart(final Date date) {
        return new DateTime(date.getTime()).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0)
                .withMillisOfSecond(0).toDate();
    }

    /**
     * Returns the last moment of the given date
     *
     * @param date Date to convert
     * @return Last moment of the given date
     */
    public static Date getDateEnd(final Date date) {
        return new DateTime(date.getTime()).withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59)
                .withMillisOfSecond(999).toDate();
    }
}
