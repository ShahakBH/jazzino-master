package com.yazino.engagement.campaign.integration;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CampaignReportingIntegrationTest {
    private SystemUnderTest sut;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 6, 28, 11, 0).getMillis());
        sut = new SystemUnderTest();
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void campaignRunsShouldBePersistedToReporting() {
        sut.createPlayers("player1", "player2");
        sut.playedYesterday("player1");
        final Long campaignId = sut.createCampaign("progressive_bonus", "this is a message", new DateTime(), Boolean.FALSE);

        sut.campaignRunConsumerReadsMessage(campaignId);
        assertThat(sut.isCampaignRunPersistedForCampaign(campaignId), is(true));

        final Map<Long, Map<String, String>> campaignRunAudits = sut.getCampaignRunAudits();
        final Map<String, String> record = campaignRunAudits.get(campaignId);

        assertThat(record.get("targetSize"), is("1"));
        assertThat(record.get("name"), is("progressive_bonus"));
        assertThat(record.get("status"), is("success"));
        assertThat(record.get("message"), is(Matchers.containsString("scheduled time:")));
    }
}
