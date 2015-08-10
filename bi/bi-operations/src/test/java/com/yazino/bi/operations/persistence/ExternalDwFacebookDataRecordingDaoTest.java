package com.yazino.bi.operations.persistence;

import com.yazino.logging.appender.ListAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalDwFacebookDataRecordingDaoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ExternalDwFacebookDataRecordingDao underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new ExternalDwFacebookDataRecordingDao(jdbcTemplate);

    }

    @Test(expected = NullPointerException.class)
    public void saveFacebookDataShouldThrowExceptionIfAdGroupStatsIsNull() {
        underTest.saveFacebookData(null, null);
    }

    @Test
    public void saveFacebookDataShouldNotCallDbIfAdGroupStatsIsEmptyCollection() {
        underTest.saveFacebookData(null, new HashMap<String, FacebookAdsStatsData>());
        verifyZeroInteractions(jdbcTemplate);
    }

    @Test
    public void saveFacebookDataShouldLogErrorAndNotRethrowItForGeneralErrors() {
        final ListAppender fallbackAppender = ListAppender.addTo(ExternalDwFacebookDataRecordingDao.class);
        HashMap<String, FacebookAdsStatsData> adGroupStats = new HashMap<String, FacebookAdsStatsData>();
        adGroupStats.put("ABCD", createFaceBookAdsStats(10, 20, 30));

        when(jdbcTemplate.batchUpdate(any(String[].class))).thenThrow(new UnsupportedOperationException("my exception"));

        underTest.saveFacebookData(null, adGroupStats);
        List messages = fallbackAppender.getMessages();

        assertTrue(messages.get(0).toString().contains("Save failed for the beans: " +
                "{ABCD=com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData"));
    }

    private FacebookAdsStatsData createFaceBookAdsStats(long clicks, long impressions, long spent) {

        FacebookAdsStatsData facebookAdsStatsData = new FacebookAdsStatsData();
        facebookAdsStatsData.setClicks(clicks);
        facebookAdsStatsData.setImpressions(impressions);
        facebookAdsStatsData.setSpent(spent);

        return facebookAdsStatsData;

    }
}
