package com.yazino.engagement.campaign.reporting.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.reporting.dao.CampaignNotificationAuditDao;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CampaignNotificationAuditConsumerTest {

    public static final DateTime CURRENT_DATE_TIME = new DateTime(2013, 6, 28, 11, 25, DateTimeZone.forID("Europe/London"));
    private CampaignNotificationAuditConsumer underTest;

    @Mock
    private CampaignNotificationAuditDao campaignNotificationAuditDao;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignNotificationAuditConsumer(campaignNotificationAuditDao);
    }

    @Test
    public void handleShouldAddMessagesToLocalThread() {
        CampaignNotificationAuditMessage campaignNotificationAuditMessage = createCampaignNotificationAuditMessage();

        assertThat(underTest.getBatchedMessages().size(), is(0));
        underTest.handle(campaignNotificationAuditMessage);
        assertThat(underTest.getBatchedMessages().size(), is(1));

    }

    @Test
    public void consumerCommittingShouldTakeMessagesFromThreadAndCommitToDao() {
        CampaignNotificationAuditMessage campaignNotificationAuditMessage = createCampaignNotificationAuditMessage();

        underTest.handle(campaignNotificationAuditMessage);
        underTest.consumerCommitting();

        verify(campaignNotificationAuditDao).persist(newHashSet(campaignNotificationAuditMessage));
    }

    private CampaignNotificationAuditMessage createCampaignNotificationAuditMessage() {

        return new CampaignNotificationAuditMessage(1234L, BigDecimal.TEN, ChannelType.IOS, "SLOTS",
                NotificationAuditType.SEND_ATTEMPT, CURRENT_DATE_TIME);

    }
}
