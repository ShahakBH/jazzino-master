package com.yazino.bi.operations.campaigns.controller;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;
import static com.yazino.engagement.campaign.domain.NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;


@RunWith(MockitoJUnitRunner.class)
public class CampaignTest {


    public static final Long CAMPAIGN_ID = 1L;
    public static final String NAME = "My Own Campaign";
    public static final Long RUN_HOURS = 2L;
    public static final Long RUN_MINUTES = 0L;
    public static final String SQL_QUERY = "select One as 1";
    private DateTime DATE_TIME = new DateTime(2013, 6, 28, 11, 22);
    private Map<String, String> contentMap;
    private List<ChannelType> channels;
    private HashMap<NotificationChannelConfigType, String> channelConfig = newHashMap();

    @Before
    public void setUp() throws Exception {
        contentMap = newLinkedHashMap();
        contentMap.put(DESCRIPTION.getKey(), "Yazino description");
        contentMap.put(MESSAGE.getKey(), "Hooray !! Yazino Message");
        contentMap.put(TRACKING.getKey(), "TRACKED_DATA");
        channelConfig.put(FILTER_OUT_120_DAY_UNOPENED, TRUE.toString());
        channels = asList(ChannelType.values());
    }

    @Test
    public void toStringMapShouldReturnCorrectMap() {
        Campaign campaign = createCampaign();

        Map<String, String> stringMap = campaign.toStringMap();

        assertThat(stringMap.get("Campaign Id"), is(CAMPAIGN_ID.toString()));
        assertThat(stringMap.get("Name"), is(NAME));
        assertThat(stringMap.get("Next Run"), is(DATE_TIME.toString()));
        assertThat(stringMap.get("Run Hours"), is(RUN_HOURS.toString()));
        assertThat(stringMap.get("Run Minutes"), is(RUN_MINUTES.toString()));
        assertThat(stringMap.get("Expire Time"), is(DATE_TIME.plusDays(1).toString()));
        assertThat(stringMap.get("SQL Query"), is(SQL_QUERY));
        assertThat(stringMap.get("Notification Channels"), is(channels.toString()));
        assertThat(stringMap.get("Delay Notification Sending"), is("true"));

        assertThat(contentMap.entrySet(), everyItem(isIn(stringMap.entrySet())));
        assertThat(campaign.getChannelConfig().get(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED),is(TRUE.toString()));

    }

    private Campaign createCampaign() {
        return new Campaign(CAMPAIGN_ID,
                NAME,
                DATE_TIME,
                DATE_TIME.plusDays(1),
                RUN_HOURS,
                RUN_MINUTES,
                SQL_QUERY,
                contentMap,
                channels,
                FALSE,
                channelConfig,
                true);
    }

}
