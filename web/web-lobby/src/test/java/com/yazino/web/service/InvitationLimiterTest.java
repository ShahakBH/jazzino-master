package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvitationLimiterTest extends TestCase {

    private static int MAX_PER_USER = 3;
    private static int MAX_PER_IP = 5;
    private static int HOURS_PER_USER = 24;
    private static int HOURS_PER_IP = 2;
    private static BigDecimal USER_ID = BigDecimal.valueOf(1234l);
    private static String USER_IP = "192.168.1.1";
    private static int MOCK_TIME = 1234567;
    private int currentMockedTime;

    private YazinoConfiguration config;
    private InvitationLimiter underTest;

    @Before
    public void setUp() {
        config = mock(YazinoConfiguration.class);
        setupDefaultLimiter();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(MOCK_TIME);
        currentMockedTime = MOCK_TIME;
    }

    @After
    public void tearDow() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testCanSendFirstInvitation() {
        assertTrue(canSendInvitations());
    }

    @Test
    public void testTrackFirstInvitation() {
        hasSentInvitations();
    }

    @Test
    public void testCantSendTooManyInvitationsPerUser() {
        assertFalse(canSendInvitations(MAX_PER_USER + 1));
    }

    @Test
    public void testCantSendTooManyInvitationsPerIp() {
        setupNoMaxPerIpLimiter();
        assertFalse(canSendInvitations(MAX_PER_IP + 1));
    }

    @Test
    public void testCanTurnOffMaxPerIp() {
        setupInvitationLimiter(HOURS_PER_USER, HOURS_PER_IP, 100, -1);
        assertTrue(canSendInvitations(99));
    }

    @Test
    public void testCanTurnOffMaxPerUser() {
        setupInvitationLimiter(HOURS_PER_USER, HOURS_PER_IP, -1, 100);
        assertTrue(canSendInvitations(99));
    }

    @Test
    public void testCanTurnOffAllLimits() {
        setupInvitationLimiter(-1, -1, -1, -1);
        assertTrue(canSendInvitations(9999));
    }

    @Test
    public void testCantSendMoreThanUserLimitInMultipleBatches() {
        setupNoMaxPerIpLimiter();
        for (int i = 0; i < MAX_PER_USER; i++) {
            assertTrue(canSendInvitations());
            hasSentInvitations();
        }
        assertFalse(canSendInvitations());
    }

    @Test
    public void testCantSendMoreThanIpLimitInMultipleBatches() {
        setupNoMaxPerUserLimiter();
        for (int i = 0; i < MAX_PER_IP; i++) {
            assertTrue(canSendInvitations());
            hasSentInvitations();
        }
        assertFalse(canSendInvitations());
    }

    @Test
    public void testTimeMovesAsExpected() {
        DateTime dt = new DateTime();
        moveClockByHours(1);
        assertEquals(currentMockedTime, new DateTime().getMillis());
        assertEquals(dt.getHourOfDay(), new DateTime().getHourOfDay() -1);
        assertEquals(dt.getMinuteOfHour(), new DateTime().getMinuteOfHour());
        assertEquals(dt.getSecondOfMinute(), new DateTime().getSecondOfMinute());
        assertEquals(dt.getMillisOfSecond(), new DateTime().getMillisOfSecond());
        moveClockByHours(1);
        assertEquals(currentMockedTime, new DateTime().getMillis());
        assertEquals(dt.getHourOfDay(), new DateTime().getHourOfDay() - 2);
        assertEquals(dt.getMinuteOfHour(), new DateTime().getMinuteOfHour());
        assertEquals(dt.getSecondOfMinute(), new DateTime().getSecondOfMinute());
        assertEquals(dt.getMillisOfSecond(), new DateTime().getMillisOfSecond());
    }

    @Test
    public void testResetPerUserCounterAfterTimeExpired() {
        setupNoMaxPerIpLimiter();
        hasSentInvitations(MAX_PER_USER);
        assertFalse(canSendInvitations());
        moveClockByHours(HOURS_PER_USER);
        assertTrue(canSendInvitations());
    }

    @Test
    public void testResetPerIpCounterAfterTimeExpired() {
        setupNoMaxPerUserLimiter();
        hasSentInvitations(MAX_PER_IP);
        assertFalse(canSendInvitations());
        moveClockByHours(HOURS_PER_IP + 1);
        assertTrue(canSendInvitations());
    }

    @Test
    public void testDefaultsToNoLimits() {
        when(config.getInt(anyString())).thenThrow(java.util.NoSuchElementException.class);
        underTest = new InvitationLimiter(config);
        assertTrue(canSendInvitations());
    }

    private boolean canSendInvitations() {
        return canSendInvitations(1);
    }

    private void hasSentInvitations() {
        hasSentInvitations(1);
    }

    private boolean canSendInvitations(final int numOfInvitationsToSend) {
        return underTest.canSendInvitations(numOfInvitationsToSend, USER_ID, USER_IP);
    }

    private void hasSentInvitations(final int numOfInvitations) {
        underTest.hasSentInvitations(numOfInvitations, USER_ID, USER_IP);
    }

    private void setupNoMaxPerUserLimiter() {
        setupInvitationLimiter(HOURS_PER_USER, HOURS_PER_IP, -1, MAX_PER_IP);
    }

    private void setupNoMaxPerIpLimiter() {
        setupInvitationLimiter(HOURS_PER_USER, HOURS_PER_IP, MAX_PER_USER, -1);
    }

    private void setupDefaultLimiter() {
        setupInvitationLimiter(HOURS_PER_USER, HOURS_PER_IP, MAX_PER_USER, HOURS_PER_IP);
    }

    private void moveClockByHours(int hours) {
        currentMockedTime += hours * 60 * 60 * 1000;
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(currentMockedTime);
    }

    private void setupInvitationLimiter(final int hoursPerUser,
                                   final int hoursPerIp,
                                   final int maxPerUser,
                                   final int maxPerIp) {
        when(config.getInt("invitationLimiter.perUser.hours")).thenReturn(hoursPerUser);
        when(config.getInt("invitationLimiter.perIp.hours")).thenReturn(hoursPerIp);
        when(config.getInt("invitationLimiter.perUser.maxAttempts")).thenReturn(maxPerUser);
        when(config.getInt("invitationLimiter.perIp.maxAttempts")).thenReturn(maxPerIp);

        underTest = new InvitationLimiter(config);
    }

}
