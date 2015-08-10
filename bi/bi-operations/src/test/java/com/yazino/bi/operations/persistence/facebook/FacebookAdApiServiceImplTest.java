package com.yazino.bi.operations.persistence.facebook;

import com.restfb.Connection;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.bi.operations.persistence.facebook.data.AdGroup;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class FacebookAdApiServiceImplTest {
    @Mock
    private FacebookClientFactory clientFactory;

    @Mock
    private FacebookClient client;

    private FacebookAdApiServiceImpl underTest;

    @Before
    public void init() {
        underTest = new FacebookAdApiServiceImpl();
        underTest.setClientFactory(clientFactory);
    }

    @Test
    public void shouldCorrectlyInitialize() {
        // GIVEN the access information
        final String accessToken = "token";
        underTest.setAccessToken(accessToken);

        // AND the factory returns a client instance
        given(clientFactory.createMapCapableGraphClient(anyString())).willReturn(client);

        // WHEN initializing the service
        underTest.init();

        // AND trying to initialize twice
        underTest.init();

        // THEN the client factory receives the correct parameters
        verify(clientFactory).createMapCapableGraphClient(accessToken);

        // AND receives them only once
        verifyNoMoreInteractions(clientFactory);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetListOfAdGroupStats() throws FacebookAdApiException {
        // GIVEN the date ranges to use
        final Calendar cal = Calendar.getInstance();
        final Date now = cal.getTime();
        cal.add(Calendar.HOUR, 1);
        final Date nowPlusHour = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        final Date minusMonthPlusHour = cal.getTime();
        cal.add(Calendar.HOUR, -1);
        final Date minusMonth = cal.getTime();

        // AND the response to the initialization is correct
        given(clientFactory.createMapCapableGraphClient(anyString())).willReturn(client);

        // AND The Facebook client returns correct data for the Facebook ad accounts...
        final String adAccount = "1234";
        underTest.setAccessToken("token");
        underTest.setAdAccount(adAccount);
        final List<AdGroup> adList = new ArrayList<AdGroup>();
        final AdGroup c1 = new AdGroup();
        c1.setId(10L);
        c1.setCampaignId("100");
        c1.setName("c10");
        adList.add(c1);
        final AdGroup c2 = new AdGroup();
        c2.setId(20L);
        c2.setCampaignId("200");
        c2.setName("C20");
        adList.add(c2);
        final Connection<AdGroup> adConn = mock(Connection.class);
        given(adConn.getData()).willReturn(adList);
        given(client.fetchConnection("act_" + adAccount + "/adgroups", AdGroup.class,
                Parameter.with("limit", 1000), Parameter.with("offset", 0), Parameter.with("fields", "id,name,campaign_id")))
                .willReturn(adConn);

        // AND ...the Facebook stats

        underTest.setTimeShift(1);

        final FacebookAdsStatsData d1 = new FacebookAdsStatsData();
        d1.setId("10");
        d1.setImpressions(100L);
        d1.setClicks(1000L);
        d1.setSpent(1L);
        final FacebookAdsStatsData d2 = new FacebookAdsStatsData();
        d2.setId("20");
        d2.setImpressions(200L);
        d2.setClicks(2000L);
        d2.setSpent(2L);

        final List<FacebookAdsStatsData> csList = Arrays.asList(d1, d2);
        final Connection<FacebookAdsStatsData> csConn = mock(Connection.class);
        given(csConn.getData()).willReturn(csList);
        given(
                client.fetchConnection("act_" + adAccount + "/adgroupstats", FacebookAdsStatsData.class,
                        Parameter.with("include_deleted", true), Parameter.with("limit", -1),
                        Parameter.with("start_time", minusMonthPlusHour.getTime() / 1000L),
                        Parameter.with("end_time", nowPlusHour.getTime() / 1000L))).willReturn(csConn);

        // WHEN the service queries for that
        final Map<String, FacebookAdsStatsData> statsMap = underTest.getAdGroupStats(minusMonth, now);

        // THEN the API is initialized
        verify(clientFactory).createMapCapableGraphClient(anyString());

        // AND the returned value matches the Facebook data
        verify(client).fetchConnection("act_" + adAccount + "/adgroupstats", FacebookAdsStatsData.class,
                Parameter.with("include_deleted", true), Parameter.with("limit", -1),
                Parameter.with("start_time", minusMonthPlusHour.getTime() / 1000L),
                Parameter.with("end_time", nowPlusHour.getTime() / 1000L));

        final Map<String, FacebookAdsStatsData> expectedMap = new HashMap<String, FacebookAdsStatsData>();
        expectedMap.put("C10", d1);
        expectedMap.put("C20", d2);
        assertEquals(expectedMap, statsMap);
    }

    @Test
    // WEB-3155 - this whole test needs refactoring. However further wrk needs to be done on ad tracking to avoid missing ad group names. This would be the time to do it.
    public void whenPopulatingMapOfAddStatsIfAdGroupNameIsMissingThenUseAddGroupIdInstead() throws FacebookAdApiException {
        // AND the response to the initialization is correct
        given(clientFactory.createMapCapableGraphClient(anyString())).willReturn(client);
        // AND The Facebook client returns correct data for the Facebook ad accounts...
        final String adAccount = "1234";
        underTest.setAccessToken("token");
        underTest.setAdAccount(adAccount);
        final List<AdGroup> adList = new ArrayList<AdGroup>();
        final AdGroup c1 = new AdGroup();
        c1.setId(10L);
        c1.setCampaignId("100");
        c1.setName("c10");
        adList.add(c1);
        final Connection<AdGroup> adConn = mock(Connection.class);
        given(adConn.getData()).willReturn(adList);
        given(
                client.fetchConnection(eq("act_" + adAccount + "/adgroups"), eq(AdGroup.class),
                        Mockito.<Parameter>anyVararg())).willReturn(adConn);

        final FacebookAdsStatsData d1 = new FacebookAdsStatsData();
        d1.setId("10");
        d1.setImpressions(100L);
        d1.setClicks(1000L);
        d1.setSpent(1L);
        final FacebookAdsStatsData d2 = new FacebookAdsStatsData();
        d2.setId("20");
        d2.setImpressions(200L);
        d2.setClicks(2000L);
        d2.setSpent(2L);

        final List<FacebookAdsStatsData> csList = Arrays.asList(d1, d2);
        final Connection<FacebookAdsStatsData> csConn = mock(Connection.class);
        given(csConn.getData()).willReturn(csList);
        given(client.fetchConnection(eq("act_" + adAccount + "/adgroupstats"), eq(FacebookAdsStatsData.class),
                Mockito.<Parameter>anyVararg())).willReturn(csConn);

        DateTime startDate = new DateTime();
        DateTime endDate = startDate.plusDays(1);
        final Map<String, FacebookAdsStatsData> adGroupStats = underTest.getAdGroupStats(startDate.toDate(),
                endDate.toDate());

        assertTrue(adGroupStats.containsKey("C10"));
        assertTrue(adGroupStats.containsKey("20"));
    }

    @Test(expected = FacebookAdApiException.class)
    public void shouldReportErrorOnFacebookFailure() throws FacebookAdApiException {
        // GIVEN the date ranges to use
        final Calendar cal = Calendar.getInstance();
        final Date now = cal.getTime();
        cal.add(Calendar.HOUR, 1);
        final Date nowPlusHour = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        final Date minusMonthPlusHour = cal.getTime();
        cal.add(Calendar.HOUR, -1);
        final Date minusMonth = cal.getTime();

        // AND the response to the initialization is correct
        given(clientFactory.createMapCapableGraphClient(anyString())).willReturn(client);

        // AND The Facebook client returns correct data for the Facebook ad accounts...
        final String adAccount = "1234";
        underTest.setAccessToken("token");
        underTest.setAdAccount(adAccount);
        given(client.fetchConnection("act_" + adAccount + "/adgroups", AdGroup.class,
                Parameter.with("limit", 1000), Parameter.with("offset", 0), Parameter.with("fields", "id,name,campaign_id")))
                .willThrow(new FacebookOAuthException("Ex", "", 123, 500));

        // WHEN the service queries for that
        final Map<String, FacebookAdsStatsData> statsMap = underTest.getAdGroupStats(minusMonth, now);

        // THEN the AdApi exception is thrown
    }
}
