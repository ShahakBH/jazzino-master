package com.yazino.web.service;

import com.yazino.platform.community.CommunityService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.service.CachingSystemMessageService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CachingSystemMessageServiceTest {
    private static final int TEN_MINUTE_INTERVAL = 600000;
    private static final String INITIAL_GIGASPACE_MESSAGE = "Initial Message from Gigaspace";
    private static final int ONE_THOUSANDTH_OF_SECOND = 1;
    private static final String REFRESHED_CACHE = "refreshed Cache";

    @Mock
    private CommunityService communityService;

    private CachingSystemMessageService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);
        underTest = new CachingSystemMessageService(communityService);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void canRetrieveSystemMessageFromGigaspaceWhenDefaultIntervalProvided() throws InterruptedException {
        when(communityService.getLatestSystemMessage()).thenReturn(INITIAL_GIGASPACE_MESSAGE).thenReturn(REFRESHED_CACHE);
        String actualSystemMessage = underTest.getLatestSystemMessage();
        assertEquals(actualSystemMessage, INITIAL_GIGASPACE_MESSAGE);
    }

    @Test
    public void canRetrieveMessageFromCacheWhenCacheLifeHasNotExpired() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);

        when(communityService.getLatestSystemMessage()).thenReturn(INITIAL_GIGASPACE_MESSAGE);
        underTest.setIntervalBetweenCaching(TEN_MINUTE_INTERVAL);
        String cachedMessage = underTest.getLatestSystemMessage();

        assertEquals(cachedMessage, INITIAL_GIGASPACE_MESSAGE);

        cachedMessage = underTest.getLatestSystemMessage();
        assertEquals(cachedMessage, INITIAL_GIGASPACE_MESSAGE);

        cachedMessage = underTest.getLatestSystemMessage();
        assertEquals(cachedMessage, INITIAL_GIGASPACE_MESSAGE);

        cachedMessage = underTest.getLatestSystemMessage();
        assertEquals(cachedMessage, INITIAL_GIGASPACE_MESSAGE);

        verify(communityService, times(1)).getLatestSystemMessage();
        assertEquals(cachedMessage, INITIAL_GIGASPACE_MESSAGE);
    }

    @Test
    public void canRefreshCacheWhenIntervalExpired() throws InterruptedException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);
        underTest.setIntervalBetweenCaching(ONE_THOUSANDTH_OF_SECOND);
        when(communityService.getLatestSystemMessage()).thenReturn(REFRESHED_CACHE);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(10 * 1000);
        String cachedMessage = underTest.getLatestSystemMessage();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(20 * 1000);
        cachedMessage = underTest.getLatestSystemMessage();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(30 * 1000);
        cachedMessage = underTest.getLatestSystemMessage();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(40 * 1000);
        cachedMessage = underTest.getLatestSystemMessage();
        verify(communityService, times(4)).getLatestSystemMessage();
        assertEquals(cachedMessage, REFRESHED_CACHE);

    }


}
