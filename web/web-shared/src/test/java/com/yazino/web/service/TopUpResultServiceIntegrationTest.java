package com.yazino.web.service;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;
import strata.server.lobby.api.promotion.TopUpResult;
import strata.server.lobby.api.promotion.message.PromotionMessage;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;

import java.math.BigDecimal;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.promotion.TopUpStatus.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class TopUpResultServiceIntegrationTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    private TopUpResultService underTest;

    @Autowired
    private CacheManager cacheManager;

    @Mock
    private DailyAwardPromotionService dailyAwardPromotionService;

    @Mock
    private QueuePublishingService<PromotionMessage> queuePublishingService;

    private Cache topUpStatusCache;

    @Before
    public void initialiseCache() {
        MockitoAnnotations.initMocks(this);

        topUpStatusCache = cacheManager.getCache("topUpStatusCache");
        topUpStatusCache.removeAll();
        topUpStatusCache.clearStatistics();
        topUpStatusCache.setStatisticsEnabled(true);

        underTest = new TopUpResultService(topUpStatusCache, dailyAwardPromotionService, queuePublishingService);
    }

    @Test
    public void getTopUpResultShouldReturnCachedResultIfPlayerHasCacheEntry(){
        topUpStatusCache.setStatisticsEnabled(true);
        long cacheHitsPreRequest = topUpStatusCache.getStatistics().getCacheHits();
        final TopUpResult expectedTopUpResult = new TopUpResult(PLAYER_ID, ACKNOWLEDGED, new DateTime());
        Element element = new Element(PLAYER_ID, new TopUpResultService.TopUpStatusCacheEntry(ACKNOWLEDGED, expectedTopUpResult.getLastTopUpDate()));
        topUpStatusCache.put(element);

        final TopUpResult actualTopUpResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        assertThat(topUpStatusCache.getStatistics().getCacheHits(), is(cacheHitsPreRequest + 1));
        assertThat(actualTopUpResult, is(expectedTopUpResult));
        verify(dailyAwardPromotionService, never()).getTopUpResult(PLAYER_ID, WEB);
    }

    @Test
    public void getTopUpResultShouldCacheResultWhenPlayerHasNoCachedResultAndLastTopUpIsAcknowledged(){
        final TopUpResult topUpResult = new TopUpResult(PLAYER_ID, ACKNOWLEDGED, new DateTime());
        when(dailyAwardPromotionService.getTopUpResult(PLAYER_ID, WEB)).thenReturn(topUpResult);

        final TopUpResult actualTopUpResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // check that result wasn't cached before
        assertThat(topUpStatusCache.getStatistics().getCacheHits(), is(0L));
        // check that result was cached after
        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(1L));
        // check cache entry
        TopUpResultService.TopUpStatusCacheEntry entry = (TopUpResultService.TopUpStatusCacheEntry)topUpStatusCache.get(PLAYER_ID).getObjectValue();
        assertThat(entry.getTopUpStatus(), is(ACKNOWLEDGED));
        assertThat(entry.getLastTopUpDate(), is(topUpResult.getLastTopUpDate()));

        assertThat(actualTopUpResult, is(topUpResult));
    }

    @Test
    public void getTopUpResultShouldDelegateToTopUpServiceAndNotCacheResultWhenPlayerHasNoCachedResultAndHasNeverBeenToppedUp(){
        final TopUpResult topUpResult = new TopUpResult(PLAYER_ID, NEVER_CREDITED, null);
        when(dailyAwardPromotionService.getTopUpResult(PLAYER_ID, WEB)).thenReturn(topUpResult);

        final TopUpResult actualTopUpResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // check that result wasn't cached before
        assertThat(topUpStatusCache.getStatistics().getCacheHits(), is(0L));
        // check that result wasn't cached after
        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(0L));

        assertThat(actualTopUpResult, is(topUpResult));
    }

    @Test
    public void getTopUpResultShouldDelegateToTopUpServiceAndNotCacheResultWhenPlayerHasNoCachedResultAndHasBeenToppedUpButNotAcknowledgedTopUp(){
        final TopUpResult topUpResult = new TopUpResult(PLAYER_ID, CREDITED, new DateTime());
        when(dailyAwardPromotionService.getTopUpResult(PLAYER_ID, WEB)).thenReturn(topUpResult);

        final TopUpResult actualTopUpResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // check that result wasn't cached before
        assertThat(topUpStatusCache.getStatistics().getCacheHits(), is(0L));
        // check that result wasn't cached after
        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(0L));

        assertThat(actualTopUpResult, is(topUpResult));
    }

    @Test
    public void acknowledgeTopUpResultShouldPutAcknowledgedResultIntoCache() {
        final TopUpAcknowledgeRequest topUpAcknowledgeRequest = new TopUpAcknowledgeRequest(PLAYER_ID, new DateTime());

        underTest.acknowledgeTopUpResult(topUpAcknowledgeRequest);

        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(1L));
        TopUpResultService.TopUpStatusCacheEntry entry = (TopUpResultService.TopUpStatusCacheEntry)topUpStatusCache.get(PLAYER_ID).getObjectValue();
        assertThat(entry.getTopUpStatus(), is(ACKNOWLEDGED));
        assertThat(entry.getLastTopUpDate(), is(topUpAcknowledgeRequest.getTopUpDate()));
    }

    @Test
    public void acknowledgeTopUpResultShouldSendAcknowledgeRequestIfNotCached() {
        final TopUpAcknowledgeRequest topUpAcknowledgeRequest = new TopUpAcknowledgeRequest(PLAYER_ID, new DateTime());

        underTest.acknowledgeTopUpResult(topUpAcknowledgeRequest);

        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(1L));
        TopUpResultService.TopUpStatusCacheEntry entry = (TopUpResultService.TopUpStatusCacheEntry)topUpStatusCache.get(PLAYER_ID).getObjectValue();
        assertThat(entry.getTopUpStatus(), is(ACKNOWLEDGED));
        assertThat(entry.getLastTopUpDate(), is(topUpAcknowledgeRequest.getTopUpDate()));
        verify(queuePublishingService).send(topUpAcknowledgeRequest);
    }

    @Test
    public void acknowledgeTopUpResultShouldNotSendAcknowledgeRequestIfAlreadyCached() {
        final TopUpAcknowledgeRequest topUpAcknowledgeRequest = new TopUpAcknowledgeRequest(PLAYER_ID, new DateTime());
        Element element = new Element(PLAYER_ID, new TopUpResultService.TopUpStatusCacheEntry(ACKNOWLEDGED, topUpAcknowledgeRequest.getTopUpDate()));
        topUpStatusCache.put(element);

        underTest.acknowledgeTopUpResult(topUpAcknowledgeRequest);

        assertThat(topUpStatusCache.getStatistics().getMemoryStoreObjectCount(), is(1L));
        TopUpResultService.TopUpStatusCacheEntry entry = (TopUpResultService.TopUpStatusCacheEntry)topUpStatusCache.get(PLAYER_ID).getObjectValue();
        assertThat(entry.getTopUpStatus(), is(ACKNOWLEDGED));
        assertThat(entry.getLastTopUpDate(), is(topUpAcknowledgeRequest.getTopUpDate()));
        verify(queuePublishingService, never()).send(topUpAcknowledgeRequest);
    }

    @Test
    public void whenNotifiedOfLogInThenTopUpResultStatusShouldBeRemovedFromCache(){
        Element element = new Element(PLAYER_ID, new TopUpResultService.TopUpStatusCacheEntry(ACKNOWLEDGED, new DateTime()));
        topUpStatusCache.put(element);
        assertNotNull(topUpStatusCache.get(PLAYER_ID));

        underTest.clearTopUpStatus(PLAYER_ID);

        assertNull(topUpStatusCache.get(PLAYER_ID));
    }
}
