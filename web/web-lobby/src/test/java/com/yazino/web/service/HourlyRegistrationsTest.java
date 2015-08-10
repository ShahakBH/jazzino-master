package com.yazino.web.service;

import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.RegistrationRecord;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HourlyRegistrationsTest {

    private HourlyRegistrations underTest;
    private Ehcache cache;
    private static final String IP_ADDRESS = "ipAddress";

    @Before
    public void setUp() throws Exception {
        cache = mock(Ehcache.class);
        underTest = new HourlyRegistrations(cache);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10000);
    }

    @After
    public void after() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldStartWithZeroRegistrations() {
        assertEquals(0, underTest.getRegistrationsFrom(IP_ADDRESS));
    }

    @Test
    public void shouldNextRegistrationInHour() {
        when(cache.get(IP_ADDRESS)).thenReturn(new Element(IP_ADDRESS, new RegistrationRecord(30, new DateTime())));
        assertEquals(30, underTest.getRegistrationsFrom(IP_ADDRESS));
    }

    @Test
    public void shouldRecordNewRegistration() {
        when(cache.get(IP_ADDRESS)).thenReturn(new Element(IP_ADDRESS, new RegistrationRecord(50, new DateTime())));
        underTest.incrementRegistrationsFrom(IP_ADDRESS);
        verify(cache).put(new Element(IP_ADDRESS, new RegistrationRecord(51, new DateTime())));
    }

    @Test
    public void shouldResetAttemptsOnNextHour() {
        when(cache.get(IP_ADDRESS)).thenReturn(new Element(IP_ADDRESS, new RegistrationRecord(100, new DateTime().minusHours(1))));
        underTest.incrementRegistrationsFrom(IP_ADDRESS);
        verify(cache).put(new Element(IP_ADDRESS, new RegistrationRecord(1, new DateTime())));
    }

    @Test
    public void shouldResetAttemptsOnSameHourNextDay() {
        when(cache.get(IP_ADDRESS)).thenReturn(new Element(IP_ADDRESS, new RegistrationRecord(100, new DateTime().minusDays(1))));
        underTest.incrementRegistrationsFrom(IP_ADDRESS);
        verify(cache).put(new Element(IP_ADDRESS, new RegistrationRecord(1, new DateTime())));
    }

    @Test
    public void shouldResetAttemptsOnNextOurOnQuery() {
        when(cache.get(IP_ADDRESS)).thenReturn(new Element(IP_ADDRESS, new RegistrationRecord(100, new DateTime().minusHours(1))));
        assertEquals(0, underTest.getRegistrationsFrom(IP_ADDRESS));
    }
}
