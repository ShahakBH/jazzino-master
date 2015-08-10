package com.yazino.platform.tournament;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the {@link DayPeriod} class.
 */
public class DayPeriodTest {

    private static final DateTimeZone LONDON_TZ = DateTimeZone.forID("Europe/London");
    private final DateTime bstStart = new DateTime(2010, DateTimeConstants.MARCH, 28, 2, 0, 0, 0, LONDON_TZ);
    private final DateTime bstEnd = new DateTime(2010, DateTimeConstants.OCTOBER, 31, 0, 0, 0, 0, LONDON_TZ);
    private final DateTime bstTime = new DateTime(2010, DateTimeConstants.APRIL, 5, 10, 30, 0, 0, LONDON_TZ);
    private final DateTime winterTime = new DateTime(2010, DateTimeConstants.DECEMBER, 25, 10, 30, 0, 0, LONDON_TZ);
    private final DateTime now = new DateTime(LONDON_TZ);

    private final DayPeriod period = new DayPeriod();

    @Before
    public void setup() {
        period.setDateTimeZone(LONDON_TZ);
    }

    @Test
    public void shouldReturnFalseWhenStartAndEndAreUndefined() throws Exception {
        assertFalse(period.isWithinPeriod(now));
    }

    @Test
    public void shouldReturnFalseWhenStartAndEndAreTheSame() throws Exception {
        period.setDay(DateTimeConstants.MONDAY);
        period.setStartTime("11:00");
        period.setEndTime("11:00");

        DateTime time = now.withDayOfWeek(DateTimeConstants.MONDAY).withTime(11, 0, 0, 0);
        assertFalse(period.isWithinPeriod(time));
    }

    @Test
    public void shouldReturnTrueWhenInTimePeriodOnSameDay() throws Exception {
        period.setDay(DateTimeConstants.MONDAY);
        period.setStartTime("11:00");
        period.setEndTime("12:00");

        DateTime time = now.withDayOfWeek(DateTimeConstants.MONDAY).withTime(11, 30, 0, 0);
        assertTrue(period.isWithinPeriod(time));
    }

    @Test
    public void shouldIncludeStartTimeSameDay() throws Exception {
        period.setDay(DateTimeConstants.MONDAY);
        period.setStartTime("11:00");
        period.setEndTime("12:00");

        DateTime time = now.withDayOfWeek(DateTimeConstants.MONDAY).withTime(11, 0, 0, 0);
        assertTrue(period.isWithinPeriod(time));
    }

    @Test
    public void shouldExcludeEndTimeSameDay() throws Exception {
        period.setDay(DateTimeConstants.MONDAY);
        period.setStartTime("11:00");
        period.setEndTime("12:00");

        DateTime time = now.withDayOfWeek(DateTimeConstants.MONDAY).withTime(12, 0, 0, 0);
        assertFalse(period.isWithinPeriod(time));
    }

    @Test
    public void shouldReturnFalseWhenEndTimeIsBeforeStartTime() throws Exception {
        period.setDay(DateTimeConstants.MONDAY);
        period.setStartTime("10:00");
        period.setEndTime("09:00");

        DateTime time = new DateTime().withDayOfWeek(DateTimeConstants.MONDAY);
        assertFalse(period.isWithinPeriod(time.withTime(11, 5, 0, 0)));
        assertFalse(period.isWithinPeriod(time.withTime(8, 55, 0, 0)));
        assertFalse(period.isWithinPeriod(time.withTime(9, 0, 0, 0)));
        assertFalse(period.isWithinPeriod(time.withTime(9, 30, 0, 0)));
        assertFalse(period.isWithinPeriod(time.withTime(10, 0, 0, 0)));
    }

    @Test
    public void shouldWorkWhenInWinterTime() throws Exception {
        period.setDay(DateTimeConstants.WEDNESDAY);
        period.setStartTime("10:00");
        period.setEndTime("10:30");
        DateTime time = winterTime.withDayOfWeek(DateTimeConstants.WEDNESDAY);
        assertFalse(period.isWithinPeriod(time.withTime(9, 59, 59, 59)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 0, 0, 0)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 1, 0, 0)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 29, 59, 59)));
        assertFalse(period.isWithinPeriod(time.withTime(10, 30, 0, 0)));
    }

    @Test
    public void shouldWorkWhenInSummerTime() throws Exception {
        period.setDay(DateTimeConstants.WEDNESDAY);
        period.setStartTime("10:00");
        period.setEndTime("10:30");
        DateTime time = bstTime.withDayOfWeek(DateTimeConstants.WEDNESDAY);
        assertFalse(period.isWithinPeriod(time.withTime(9, 59, 59, 59)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 0, 0, 0)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 1, 0, 0)));
        assertTrue(period.isWithinPeriod(time.withTime(10, 29, 59, 59)));
        assertFalse(period.isWithinPeriod(time.withTime(10, 30, 0, 0)));
    }

    @Test
    public void shouldWorkWhenMovingFromWinterToSummerTime_1() throws Exception {
        period.setDay(DateTimeConstants.SUNDAY);
        period.setStartTime("01:00");
        period.setEndTime("01:30");

        assertFalse(period.isWithinPeriod(bstStart.withTime(0, 59, 59, 59)));
        assertFalse(period.isWithinPeriod(bstStart.minusDays(1).withTime(23, 59, 59, 59)));
        assertFalse(period.isWithinPeriod(bstStart));
    }

    @Test
    public void shouldWorkWhenMovingFromWinterToSummerTime_2() throws Exception {
        period.setDay(DateTimeConstants.SUNDAY);
        period.setStartTime("00:00");
        period.setEndTime("01:30");

        assertTrue(period.isWithinPeriod(bstStart.withTime(0, 0, 0, 0)));
        assertTrue(period.isWithinPeriod(bstStart.withTime(0, 59, 59, 59)));
        assertTrue(period.isWithinPeriod(bstStart.minusHours(1)));
    }

    @Test
    public void shouldWorkWhenMovingFromSummerToWinterTime() throws Exception {
        period.setDay(DateTimeConstants.SUNDAY);
        period.setStartTime("01:00");
        period.setEndTime("01:30");

        assertFalse(period.isWithinPeriod(bstEnd.minusDays(1).withTime(23, 59, 59, 59)));
        assertFalse(period.isWithinPeriod(bstEnd.withTime(00, 59, 59, 59)));
        assertFalse(period.isWithinPeriod(bstEnd));

        assertTrue(period.isWithinPeriod(bstEnd.plusHours(1)));
        assertTrue(period.isWithinPeriod(bstEnd.plusHours(2)));
    }

    @Test
    public void shouldSetDayCorrectlyWhenUsingString() throws Exception {
        period.setDayName("Tuesday");
        assertEquals(DateTimeConstants.TUESDAY, period.getDay());
    }

    @Test
    public void shouldRetrieveDayNameCorrectly() throws Exception {
        period.setDay(DateTimeConstants.TUESDAY);
        assertEquals("Tuesday", period.getDayName());
    }

    @Test
    public void shouldParseConstructorCorrectly() throws Exception {
        DayPeriod period = new DayPeriod("Wednesday@09:00-18:30");
        assertEquals(DateTimeConstants.WEDNESDAY, period.getDay());
        assertEquals(new LocalTime(9, 0), period.getStartTime());
        assertEquals(new LocalTime(18, 30), period.getEndTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWrongFormatPassedToConstructor_day() throws Exception {
        new DayPeriod("@09:00-18:30");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWrongFormatPassedToConstructor_start() throws Exception {
        new DayPeriod("Wednesday@:00-18:30");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWrongFormatPassedToConstructor_end() throws Exception {
        new DayPeriod("Wednesday@09:00-99:30");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWrongFormatPassedToConstructor_daySeparator() throws Exception {
        new DayPeriod("Wednesday/09:00-18:30");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenWrongFormatPassedToConstructor_hourSeparator() throws Exception {
        new DayPeriod("Wednesday@09;00-18;30");
    }

    @Test
    public void shouldFormatOutputStringCorrectly() throws Exception {
        period.setDay(DateTimeConstants.THURSDAY);
        period.setStartTime("09:45");
        period.setEndTime("12:50");

        assertEquals("Thursday@09:45-12:50", period.toFormattedPeriod());
    }

}