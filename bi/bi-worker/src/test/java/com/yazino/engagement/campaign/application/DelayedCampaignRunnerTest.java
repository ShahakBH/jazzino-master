package com.yazino.engagement.campaign.application;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class DelayedCampaignRunnerTest {
    public static final long CAMPAIGN_RUN_ID_1 = 123L;
    public static final long CAMPAIGN_RUN_ID_2 = 321L;
    public static final long CAMPAIGN_ID_1 = 333L;
    public static final long CAMPAIGN_ID_2 = 666L;
    private DelayedCampaignRunner underTest;
    @Mock
    private CampaignScheduler campaignScheduler;

    @Mock
    private SegmentSelectorDao segmentSelectorDao;
    @Mock
    private QueuePublishingService<CampaignDeliverMessage> campaignDeliverMessageQueuePublishingService;
    @Mock
    private CampaignRunDao campaignRunDao;

    @Mock
    private CampaignDefinitionDao campaignDefinitionDao;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new DelayedCampaignRunner(
                segmentSelectorDao,
                campaignDeliverMessageQueuePublishingService,
                campaignRunDao, campaignDefinitionDao);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now().withMillisOfSecond(0).getMillis());

    }


    @Test
    public void rerunShouldUpdateSegmentSelectionsForCampaignsThatAreUsingDelayedSend() {
        when(campaignRunDao.getLatestDelayedCampaignRunsInLast24Hours()).thenReturn(
                ImmutableMap.of(CAMPAIGN_RUN_ID_1, CAMPAIGN_ID_1,
                                CAMPAIGN_RUN_ID_2, CAMPAIGN_ID_2));
        underTest.reRun();
        verify(segmentSelectorDao).updateSegmentDelaysForCampaignRuns(
                ImmutableSet.of(CAMPAIGN_RUN_ID_2, CAMPAIGN_RUN_ID_1), now());

    }

    @Test
    public void rerunShouldCreateNewCampaignRunMessages() {

        ArgumentCaptor<CampaignDeliverMessage> argCap = ArgumentCaptor.forClass(CampaignDeliverMessage.class);
        when(campaignDefinitionDao.getChannelTypes(CAMPAIGN_ID_1)).thenReturn(of(ChannelType.AMAZON_DEVICE_MESSAGING));
        when(campaignDefinitionDao.getChannelTypes(CAMPAIGN_ID_2)).thenReturn(of(ChannelType.IOS));

        when(campaignRunDao.getLatestDelayedCampaignRunsInLast24Hours()).thenReturn(
                ImmutableMap.of(CAMPAIGN_RUN_ID_1, CAMPAIGN_ID_1,
                                CAMPAIGN_RUN_ID_2, CAMPAIGN_ID_2));

        when(campaignRunDao.getLastRuntimeForCampaignRunIdAndResetTo(CAMPAIGN_RUN_ID_1, now())).thenReturn(
                now().minusHours(1));
        when(campaignRunDao.getLastRuntimeForCampaignRunIdAndResetTo(CAMPAIGN_RUN_ID_2, now())).thenReturn(
                now().minusHours(2));

        underTest.reRun();

        verify(campaignDeliverMessageQueuePublishingService, times(2)).send(argCap.capture());
        final List<CampaignDeliverMessage> messages = argCap.getAllValues();
        assertThat(messages.size(), is(2));

    }

}