package com.yazino.bi.operations.persistence.facebook;

import static org.junit.Assert.assertTrue;
import static strata.server.test.helpers.classes.ClassStructureTestSupport.testDefaultConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.yazino.bi.operations.persistence.facebook.data.Campaign;
import com.yazino.bi.operations.persistence.facebook.data.FacebookAdsStatsData;
import com.yazino.bi.operations.persistence.facebook.data.TimeRange;

import com.restfb.FacebookClient;
import com.restfb.Parameter;

public class FacebookClientFactoryTestIntegration {
    @Test
    public void shouldCreateMapCapableGraphClient() throws IllegalAccessException, InvocationTargetException,
            InstantiationException {
        // GIVEN the client factory and the access information
        final String accessToken =
                "AAACnnOmLRwMBAOkFZANA3poiBjh8H7qJhi9EoSrDmNVvSbxC0LZAG02ScF5ogyJMui3F3iXLOlgpZC9R7VZCKzHq6pWDm8QZD";
        final String accountId = "354857149";

        // WHEN the client's instance is requested
        final FacebookClient client =
                FacebookExtendedClientFactoryImpl.getInstance().createMapCapableGraphClient(accessToken);

        // THEN it is correctly configured and returned
        final List<Campaign> campaigns =
                client.fetchConnection("act_" + accountId + "/adcampaigns", Campaign.class,
                        Parameter.with("limit", "1"), Parameter.with("offset", "0"),
                        Parameter.with("include_count", false)).getData();
        assertTrue(campaigns.size() > 0);
        final List<String> campaignList = new ArrayList<String>();
        campaignList.add(campaigns.get(0).getCampaignId());
        final List<TimeRange> timesList = new ArrayList<TimeRange>();
        final Calendar move = Calendar.getInstance();
        move.add(Calendar.MONTH, -1);
        timesList.add(new TimeRange(move.getTime(), new Date()));
        final List<FacebookAdsStatsData> stats =
                client.fetchConnection("act_" + accountId + "/adcampaignstats", FacebookAdsStatsData.class,
                        Parameter.with("include_deleted", true),
                        Parameter.with("start_time", move.getTime().getTime() / 1000L),
                        Parameter.with("end_time", new Date().getTime() / 1000L)).getData();
        assertTrue(stats.size() > 0);

        // AND the default constructor of the factory exists
        assertTrue(testDefaultConstructor(FacebookExtendedClientFactoryImpl.class));
    }
}
