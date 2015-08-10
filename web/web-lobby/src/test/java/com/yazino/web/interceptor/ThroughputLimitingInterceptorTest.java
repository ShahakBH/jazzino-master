package com.yazino.web.interceptor;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.logging.appender.ListAppender;
import com.yazino.web.util.MinuteBoundedCounter;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThroughputLimitingInterceptorTest {

    public static final int MAX_HITS_PER_MINUTE = 10;

    private MinuteBoundedCounter minuteBoundedCounter = mock(MinuteBoundedCounter.class);
    private YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
    private ThroughputLimitingInterceptor underTest = new ThroughputLimitingInterceptor(yazinoConfiguration, minuteBoundedCounter);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private Object handler = new Object();
    public static final int BLOCK_ALL_TRACKING_REQUESTS = 0;
    private ListAppender listAppender;

    @Before
    public void setUp() throws Exception {
        when(request.getRequestURI()).thenReturn("/tracking/event");
        when(yazinoConfiguration.getInt("tracking.max-hits-per-minute")).thenReturn(MAX_HITS_PER_MINUTE);
        when(yazinoConfiguration.getInt("tracking.logging.frequency.blocked-requests")).thenReturn(1000);
        listAppender = ListAppender.addTo(ThroughputLimitingInterceptor.class);
    }

    @Test
    public void shouldReturnTrueWhileTrafficCountDoesNotExceedMaxHitsPerMinute() throws Exception {
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(MAX_HITS_PER_MINUTE);
        assertAllowingTraffic();
    }

    @Test
    public void shouldBlockRequestWhenTrafficCountExceedsMaxHitsPerMinute() throws Exception {
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(MAX_HITS_PER_MINUTE + 1);
        assertBlockingTraffic();
    }

    @Test
    public void shouldNotLogAllowedRequests() throws Exception {
        when(yazinoConfiguration.getInt("tracking.max-hits-per-minute")).thenReturn(2000); // raise threshold to check not logging every 1000 while not blocking
        underTest = new ThroughputLimitingInterceptor(yazinoConfiguration, minuteBoundedCounter);
        ListAppender listAppender = ListAppender.addTo(ThroughputLimitingInterceptor.class);
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(1000);

        assertAllowingTraffic();
        assertTrue(listAppender.getMessages().isEmpty());
    }

    @Test
    public void shouldLogEveryNRequestsWhenBlocking() throws Exception {
        assertDoesNotLogNthBlockedRequest(999);
        assertLogsNthBlockedRequest(1000);
        assertDoesNotLogNthBlockedRequest(1001);
        assertLogsNthBlockedRequest(2000);
    }

    private void assertLogsNthBlockedRequest(int n) throws Exception {
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(n);
        listAppender.clear();
        underTest.preHandle(request, response, handler);
        assertEquals(1, listAppender.getMessages().size());
    }

    private void assertDoesNotLogNthBlockedRequest(int n) throws Exception {
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(n);
        listAppender.clear();
        underTest.preHandle(request, response, handler);
        assertEquals(0, listAppender.getMessages().size());
    }

    @Test
    public void shouldIgnoreNonTrackingRequests() throws Exception {
        when(request.getRequestURI()).thenReturn("/non-tracking/event");
        underTest = new ThroughputLimitingInterceptor(yazinoConfiguration, minuteBoundedCounter);

        assertAllowingTraffic();
    }

    @Test
    public void shouldUseTheCurrentlyConfiguredValueOfMaxHitsPerMinute() throws Exception {
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(MAX_HITS_PER_MINUTE + 1);
        assertBlockingTraffic();
        when(yazinoConfiguration.getInt("tracking.max-hits-per-minute")).thenReturn(MAX_HITS_PER_MINUTE + 1);
        assertAllowingTraffic();
        when(minuteBoundedCounter.incrementAndGet()).thenReturn(MAX_HITS_PER_MINUTE + 2);
        assertBlockingTraffic();
    }

    @Test
    public void shouldUseTheCurrentlyConfiguredValueOfLoggingFrequency() throws Exception {
        when(yazinoConfiguration.getInt("tracking.max-hits-per-minute")).thenReturn(BLOCK_ALL_TRACKING_REQUESTS);
        when(yazinoConfiguration.getInt("tracking.logging.frequency.blocked-requests")).thenReturn(1);
        assertLogsNthBlockedRequest(1);
        assertLogsNthBlockedRequest(2);
        when(yazinoConfiguration.getInt("tracking.logging.frequency.blocked-requests")).thenReturn(10);
        assertDoesNotLogNthBlockedRequest(9);
        assertLogsNthBlockedRequest(10);
        assertDoesNotLogNthBlockedRequest(11);
    }

    private void assertAllowingTraffic() throws Exception {
        boolean continueProcessing = underTest.preHandle(request, response, handler);
        assertTrue(continueProcessing);
    }

    private void assertBlockingTraffic() throws Exception {
        boolean continueProcessing = underTest.preHandle(request, response, handler);
        assertFalse(continueProcessing);
    }

}
