package com.yazino.platform.model.tournament;

import com.yazino.platform.tournament.DayPeriod;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTimeConstants.*;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link com.yazino.platform.model.tournament.RecurringTournamentDefinition} class.
 */
public class RecurringTournamentDefinitionTest {

    private final DateTime testTime = new DateTime(2012, 2, 15, 12, 0, 0, DateTimeZone.forID("UTC"));

    private final RecurringTournamentDefinition definition = new RecurringTournamentDefinition();

    @Test
    public void shouldReturnEmptyWhenSignupTimeInPastAndNoFrequency() throws Exception {
        definition.setInitialSignupTime(testTime.minusDays(1));
        definition.setFrequency(0L);
        Assert.assertEquals(0, definition.calculateSignupTimes(new Interval(testTime, testTime.plusDays(1))).size());
    }

    @Test
    public void shouldReturnEmptyWhenSignupTimeOutsideInterval() throws Exception {
        definition.setInitialSignupTime(testTime.plusDays(2));
        definition.setFrequency(0L);
        Assert.assertEquals(0, definition.calculateSignupTimes(new Interval(testTime, testTime.plusDays(1))).size());
    }

    @Test
    public void shouldReturnEmptyWhenSignupTimeAtIntervalLimit() throws Exception {
        definition.setInitialSignupTime(testTime.plusDays(1));
        definition.setFrequency(0L);
        Assert.assertEquals(0, definition.calculateSignupTimes(new Interval(testTime, testTime.plusDays(1))).size());
    }

    @Test
    public void shouldReturnEmptywhenSignupInsideIntervalButExcluded() throws Exception {
        DateTime signupStart = testTime.withTime(15, 30, 0, 0);
        DayPeriod exclusionPeriod = new DayPeriod();
        exclusionPeriod.setDay(signupStart.getDayOfWeek());
        exclusionPeriod.setStartTime("15:00");
        exclusionPeriod.setEndTime("16:00");
        definition.setInitialSignupTime(signupStart);
        definition.setFrequency(0L);
        definition.setExclusionPeriods(exclusionPeriod);

        Assert.assertEquals(0, definition.calculateSignupTimes(new Interval(signupStart.minusMinutes(30), signupStart.plusDays(1))).size());
    }

    @Test
    public void shouldReturnSingleEntryWhenSignupTimeInFutureAndNoFrequency() throws Exception {
        definition.setInitialSignupTime(testTime.plusMinutes(5));
        definition.setFrequency(0L);
        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(testTime, testTime.plusDays(1)));
        Assert.assertEquals(1, signupTimes.size());
        Assert.assertEquals(definition.getInitialSignupTime(), signupTimes.iterator().next());
    }

    @Test
    public void shouldReturnSingleEntryWhenSignupTimeInFutureAndNextFrequencyOutsideInterval() throws Exception {
        definition.setInitialSignupTime(testTime.plusMinutes(5));
        definition.setFrequency(2 * (long) MILLIS_PER_DAY);
        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(testTime, testTime.plusDays(1)));
        Assert.assertEquals(1, signupTimes.size());
        Assert.assertEquals(definition.getInitialSignupTime(), signupTimes.iterator().next());
    }

    @Test
    public void shouldReturnIntervalDividedByFrequencyEntriesWhenSignupInPast() throws Exception {
        DateTime intervalStart = testTime.withTime(15, 0, 0, 0);
        definition.setInitialSignupTime(intervalStart.minusYears(1));
        definition.setFrequency(10 * (long) MILLIS_PER_MINUTE);
        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(intervalStart, intervalStart.plusHours(1)));
        Assert.assertEquals(6, signupTimes.size());
        for (int i = 0; i < 6; i++) {
            assertTrue(signupTimes.contains(intervalStart.plus(i * definition.getFrequency())));
        }
    }

    @Test
    public void shouldReturnEndPeriodMinusStartTimeDividedByFrequencyEntriesWhenSignupInInterval() throws Exception {
        DateTime intervalStart = testTime.withTime(15, 0, 0, 0);
        definition.setInitialSignupTime(intervalStart.plusMinutes(25));
        definition.setFrequency(10L * MILLIS_PER_MINUTE);
        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(intervalStart, intervalStart.plusHours(1)));
        Assert.assertEquals(4, signupTimes.size());
        for (int i = 0; i < 4; i++) {
            assertTrue(signupTimes.contains(definition.getInitialSignupTime().plus(i * definition.getFrequency())));
        }
    }

    @Test
    public void shouldWork() throws Exception {
        DateTime sixHoursAgo = testTime.minusHours(6);

        definition.setInitialSignupTime(sixHoursAgo);
        definition.setFrequency(1L * MILLIS_PER_DAY);
        definition.setSignupPeriod(30L * MILLIS_PER_MINUTE);
        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(testTime, testTime.plusHours(24)));
        Assert.assertEquals(1, signupTimes.size());
        Assert.assertEquals(sixHoursAgo.plus(definition.getFrequency()), signupTimes.iterator().next());
    }


    @Test
    public void shouldReturnEntriesWhenSignupTimeInPastAndFrequencyOkButSomeInExcludedPeriod() throws Exception {

        DateTime intervalStart = testTime.withTime(15, 30, 0, 0);
        DayPeriod exclusionPeriod = new DayPeriod();
        exclusionPeriod.setDay(intervalStart.getDayOfWeek());
        exclusionPeriod.setStartTime("15:00");
        exclusionPeriod.setEndTime("16:00");

        definition.setExclusionPeriods(exclusionPeriod);
        definition.setInitialSignupTime(intervalStart.minusDays(1));
        definition.setFrequency(10 * (long) MILLIS_PER_MINUTE);

        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(intervalStart, intervalStart.plusHours(1)));
        Assert.assertEquals(3, signupTimes.size());
        for (int i = 0; i < 3; i++) {
            assertTrue(signupTimes.contains(intervalStart.withTime(16, 0, 0, 0).plus(i * definition.getFrequency())));
        }

    }

    @Test
    public void shouldReturnEmptyWhenAllSignupsInIntervalExcluded() throws Exception {
        DateTime intervalStart = testTime.withTime(15, 30, 0, 0);
        DayPeriod exclusionPeriodA = new DayPeriod();
        exclusionPeriodA.setDay(intervalStart.getDayOfWeek());
        exclusionPeriodA.setStartTime("15:00");
        exclusionPeriodA.setEndTime("16:00");
        DayPeriod exclusionPeriodB = new DayPeriod();
        exclusionPeriodB.setDay(intervalStart.getDayOfWeek());
        exclusionPeriodB.setStartTime("16:00");
        exclusionPeriodB.setEndTime("17:00");

        definition.setExclusionPeriods(exclusionPeriodA, exclusionPeriodB);
        definition.setInitialSignupTime(intervalStart.minusDays(1));
        definition.setFrequency(10 * (long) MILLIS_PER_MINUTE);

        Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(intervalStart, intervalStart.plusHours(1)));
        Assert.assertEquals(0, signupTimes.size());
    }

    @Test
    public void zeroFrequency_initialDateBeforeInterval_returnsZeroEntries() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));

        DateTime oneWeekAgo = testTime.minusWeeks(1);

        definition.setFrequency(0L);
        definition.setInitialSignupTime(oneWeekAgo);

        verifySignupTimes(definition, lookahead);
    }

    @Test
    public void zeroFrequency_initalDateOnIntervalStart_returnsInitialDate() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));

        definition.setFrequency(0L);
        definition.setInitialSignupTime(testTime);

        verifySignupTimes(definition, lookahead, testTime);
    }

    @Test
    public void zeroFrequency_initialDateWithinInterval_returnsInitialDate() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        DateTime twoHoursFromNow = testTime.plusHours(2);

        definition.setFrequency(0L);
        definition.setInitialSignupTime(twoHoursFromNow);

        verifySignupTimes(definition, lookahead, twoHoursFromNow);
    }

    @Test
    public void zeroFrequency_initialDateOnIntervalEnd_returnsZeroEntries() throws Exception {
        DateTime oneDayFromNow = testTime.plusDays(1);
        Interval lookahead = new Interval(testTime, oneDayFromNow);

        definition.setFrequency(0L);
        definition.setInitialSignupTime(oneDayFromNow);

        verifySignupTimes(definition, lookahead);
    }

    @Test
    public void zeroFrequency_initialDateAfterInterval_returnsZeroEntries() throws Exception {
        DateTime oneDayFromNow = testTime.plusDays(1);
        DateTime twoDaysFromNow = testTime.plusDays(2);
        Interval lookahead = new Interval(testTime, oneDayFromNow);

        definition.setFrequency(0L);
        definition.setInitialSignupTime(twoDaysFromNow);

        verifySignupTimes(definition, lookahead);
    }

    @Test
    public void frequencyDividesIntoIntervalOnce_initialDateBeforeInterval_returnsOneSignup() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        long twentyHours = 20L * MILLIS_PER_HOUR;
        DateTime sixHoursAgo = testTime.minusHours(6);

        definition.setFrequency(twentyHours);
        definition.setInitialSignupTime(sixHoursAgo);

        DateTime expectedSignup = testTime.plusHours(20 - 6);

        verifySignupTimes(definition, lookahead, expectedSignup);
    }


    @Test
    public void frequencyDividesIntoIntervalOnce_initialDateBeforeIntervalBySeveralFrequencies_returnsOneSignup() throws Exception {
        final int intervalInHours = 20;
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        DateTime sixDaysAndOneHourAgo = testTime.minusDays(6).minusHours(1);

        definition.setFrequency((long) intervalInHours * MILLIS_PER_HOUR);
        definition.setInitialSignupTime(sixDaysAndOneHourAgo);

        DateTime expectedSignup = testTime.plusHours(intervalInHours - ((6 * DateTimeConstants.HOURS_PER_DAY + 1) % intervalInHours));
        verifySignupTimes(definition, lookahead, expectedSignup);
    }

    @Test
    public void frequencyDividesIntoIntervalOnce_initialDateOnIntervalStart_returnsTwoSignups() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        long twentyHours = 20L * MILLIS_PER_HOUR;

        definition.setFrequency(twentyHours);
        definition.setInitialSignupTime(testTime);

        DateTime expectedSignup = testTime.plusHours(20);

        verifySignupTimes(definition, lookahead, testTime, expectedSignup);
    }

    @Test
    public void frequencyDividesIntoIntervalOnce_initialDateInIntervalButSecondOccurranceOut_returnsOneSignup() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        long twentyHours = 20L * MILLIS_PER_HOUR;

        definition.setFrequency(twentyHours);
        definition.setInitialSignupTime(testTime.plusHours(6));

        DateTime expectedSignup = definition.getInitialSignupTime();

        verifySignupTimes(definition, lookahead, expectedSignup);
    }

    @Test
    public void frequencyDividesIntoIntervalOnce_initialDateInInterval_returnsTwoSignups() throws Exception {
        Interval lookahead = new Interval(testTime, testTime.plusDays(1));
        long twentyHours = 20L * MILLIS_PER_HOUR;

        definition.setFrequency(twentyHours);
        definition.setInitialSignupTime(testTime.plusHours(2));

        DateTime expectedSignup = testTime.plusHours(20 + 2);

        verifySignupTimes(definition, lookahead, definition.getInitialSignupTime(), expectedSignup);
    }


    private static void verifySignupTimes(RecurringTournamentDefinition definition, Interval interval, DateTime... expected) {
        final Set<DateTime> signupTimes = definition.calculateSignupTimes(interval);
        assertThat(signupTimes.size(), is(equalTo(expected.length)));
        for (DateTime signup : expected) {
            assertThat(signupTimes, hasItem(signup));
        }
    }

}
