package com.yazino.bi.operations.persistence.facebook;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;

import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class FacebookAdApiServiceExternalIntegrationTest {
    @Autowired
    private FacebookAdApiServiceImpl underTest;

    @Test(timeout = 60000)
    public void shouldContactServiceAndGetAppropriateResponseForAds() throws FacebookAdApiException {
        // this needs to be a date we have data for
        final DateTime targetDateMidnight = new DateTime(2012, 9, 21, 0, 0, 0, 0);
        final Date startTime = targetDateMidnight.toDate();
        final Date endTime = targetDateMidnight.minusDays(1).toDate();

        // AND the standard time shift
        underTest.setTimeShift(-1);

        // WHEN getting the statistics map
        final Map<String, FacebookAdsStatsData> statsMap = underTest.getAdGroupStats(endTime, startTime);

        // THEN the map is correctly filled
        assertTrue(statsMap.size() > 0);
    }


}
