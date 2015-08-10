package com.yazino.util;

import org.joda.time.DateTimeUtils;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class provides some date utilities, mainly useful for populating things such as date fields.
 */
public class DateUtils {
    private static final int MONTHS_IN_YEAR = 12;

    private final DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance();

    public String[] getShortFormMonthsOfYear() {
        return Arrays.copyOf(dateFormatSymbols.getShortMonths(), MONTHS_IN_YEAR);
    }

    public int[] getYearsUntil(final int endYear) {
        final Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(DateTimeUtils.currentTimeMillis());
        final int currentYear = calendar.get(Calendar.YEAR);
        return getRange(currentYear, endYear);
    }

    private static int[] getRange(final int x,
                                  final int y) {
        int[] range;
        if (x > y) {
            range = new int[(x - y) + 1];
            int index = 0;
            for (int i = x; i >= y; i--) {
                range[index++] = i;
            }
        } else {
            range = new int[(y - x) + 1];
            int index = 0;
            for (int i = x; i <= y; i++) {
                range[index++] = i;
            }
        }
        return range;
    }
}
