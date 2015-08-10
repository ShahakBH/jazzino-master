package com.yazino.bi.operations.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import com.yazino.bi.operations.persistence.FacebookDataRecordingDao;
import com.yazino.bi.operations.persistence.facebook.FacebookAdApiException;
import com.yazino.bi.operations.persistence.facebook.FacebookAdApiService;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.collect.ImmutableMap.of;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FacebookDataRecoveryServiceTest {
    private FacebookDataRecoveryService underTest;

    @Mock
    private FacebookDataRecordingDao dwDao;
    @Mock
    private FacebookDataRecordingDao externalDwDao;

    @Mock
    private FacebookAdApiService adApi;

    private Date today = new DateTime().withTime(0, 0, 0, 0).toDate();

    private Date yesterday = new DateTime().minusDays(1).withTime(0, 0, 0, 0).toDate();

    @Before
    public void init() throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {

        underTest = new FacebookDataRecoveryService();

        ReflectionTestUtils.setField(underTest, "adApi", adApi);
        ReflectionTestUtils.setField(underTest, "dwDao", dwDao);
        ReflectionTestUtils.setField(underTest, "externalDwDao", externalDwDao);
    }

    @Test
    public void shouldFillDataTillToday() throws FacebookAdApiException {
        given(dwDao.getLatestRecordDate()).willReturn(yesterday);

        // AND Facebook correctly returns data for two last days
        final FacebookAdsStatsData yesterdayData = new FacebookAdsStatsData();
        final FacebookAdsStatsData todayData = new FacebookAdsStatsData();

        fillFacebookDataAndAssumeItReturned(today, yesterday, yesterdayData, todayData);

        final Map<String, FacebookAdsStatsData> yesterdayDataMap = new TreeMap<String, FacebookAdsStatsData>();
        yesterdayDataMap.put("B", yesterdayData);

        final Map<String, FacebookAdsStatsData> todayDataMap = new TreeMap<String, FacebookAdsStatsData>();
        todayDataMap.put("A", todayData);

        underTest.fillFacebookData();

        verify(dwDao).getLatestRecordDate();
        verify(dwDao).saveFacebookData(yesterday, yesterdayDataMap);
        verify(dwDao).saveFacebookData(today, todayDataMap);
        verifyNoMoreInteractions(dwDao);
    }

    private void fillFacebookDataAndAssumeItReturned(final Date today,
                                                     final Date yesterday, final FacebookAdsStatsData yesterdayData,
                                                     final FacebookAdsStatsData todayData) throws FacebookAdApiException {
        yesterdayData.setClicks(101L);
        yesterdayData.setImpressions(201L);
        yesterdayData.setSpent(1501L);
        todayData.setClicks(100L);
        todayData.setImpressions(200L);
        todayData.setSpent(1500L);
        given(adApi.getAdGroupStats(today, new DateTime(today).plusDays(1).toDate())).willReturn(of("A", todayData));
        given(adApi.getAdGroupStats(yesterday, today)).willReturn(of("B", yesterdayData));
    }

    @Test
    public void shouldFillDataFromHardCodedStartDate()
            throws FacebookAdApiException {
        // GIVEN there is no data in the DB
        given(dwDao.getLatestRecordDate()).willReturn(null);

        // AND Facebook returns some data
        final FacebookAdsStatsData yesterdayData = new FacebookAdsStatsData();
        final FacebookAdsStatsData todayData = new FacebookAdsStatsData();

        final Map<String, FacebookAdsStatsData> yesterdayDataMap = new TreeMap<String, FacebookAdsStatsData>();
        yesterdayDataMap.put("B", yesterdayData);

        final Map<String, FacebookAdsStatsData> todayDataMap = new TreeMap<String, FacebookAdsStatsData>();
        todayDataMap.put("A", todayData);

        fillFacebookDataAndAssumeItReturned(today, yesterday, yesterdayData, todayData);

        // WHEN asking the service to fill the DB
        underTest.fillFacebookData();

        // THEN the records for two last days are written
        verify(dwDao).getLatestRecordDate();
        verify(dwDao).saveFacebookData(yesterday, yesterdayDataMap);
        verify(dwDao).saveFacebookData(today, todayDataMap);
    }

    @Test
    public void fillFacebookDataShouldCallDwAndExternalDwToSaveData() throws FacebookAdApiException {

        given(dwDao.getLatestRecordDate()).willReturn(yesterday);
        Map<String, FacebookAdsStatsData> adGroupStats = new HashMap<String, FacebookAdsStatsData>();
        adGroupStats.put("ABCD", new FacebookAdsStatsData());
        given(adApi.getAdGroupStats(yesterday, today)).willReturn(adGroupStats);

        underTest.fillFacebookData();

        verify(dwDao).saveFacebookData(yesterday, adGroupStats);
        verify(externalDwDao).saveFacebookData(yesterday, adGroupStats);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fillFacebookDataShouldNotCallDwAndExternalDwToSaveDataIfFacebookReturnsEmptyMap() throws
            FacebookAdApiException {

        given(dwDao.getLatestRecordDate()).willReturn(yesterday);
        Map<String, FacebookAdsStatsData> adGroupStats = new HashMap<String, FacebookAdsStatsData>();
        given(adApi.getAdGroupStats(yesterday, today)).willReturn(adGroupStats);

        underTest.fillFacebookData();

        verify(dwDao, never()).saveFacebookData(eq(yesterday), any(Map.class));
        verifyZeroInteractions(externalDwDao);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fillFacebookDataShouldNotBubbleUpErrors() throws FacebookAdApiException {

        given(dwDao.getLatestRecordDate()).willReturn(yesterday);
        given(adApi.getAdGroupStats(yesterday, today)).willThrow(new FacebookAdApiException("", new RuntimeException()));

        underTest.fillFacebookData();

        verify(dwDao, never()).saveFacebookData(eq(yesterday), any(Map.class));
        verifyZeroInteractions(externalDwDao);
    }
}
