package com.yazino.engagement.campaign.application;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EmailCampaignDeliverMessage;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.email.infrastructure.CampaignCommanderClient;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailCampaignUploaderAdapterTest {
    private static final long CAMPAIGN_RUN_ID = 1l;
    private EmailCampaignUploaderAdapter underTest;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private CampaignNotificationDao campaignNotificationDao;
    @Mock
    private CampaignCommanderClient campaignCommanderClient;
    @Mock
    private CampaignContentService campaignContentService;
    @Mock
    private QueuePublishingService<EmailCampaignDeliverMessage> queuePublishingService;
    private CampaignDeliverMessage campaignDeliverMessage;
    private List<EmailTarget> emailTargets;

    @Before
    public void setUp() {
        underTest = new EmailCampaignUploaderAdapter(campaignNotificationDao, campaignCommanderClient, campaignContentService,
                queuePublishingService, yazinoConfiguration);
        emailTargets = newArrayList();
        when(campaignNotificationDao.getEligibleEmailTargets(CAMPAIGN_RUN_ID)).thenReturn(emailTargets);
        campaignDeliverMessage = new CampaignDeliverMessage(CAMPAIGN_RUN_ID, ChannelType.EMAIL);
        when(yazinoConfiguration.getBoolean("emailvision.campaign.enabled", Boolean.FALSE)).thenReturn(true);
    }

    @Test
    public void whenDeliverIsDisabledShouldNotAddMessageToDeliverQueue() {
        when(yazinoConfiguration.getBoolean("emailvision.campaign.deliver")).thenReturn(false);
        underTest.sendMessageToPlayers(campaignDeliverMessage);
        verifyNoMoreInteractions(queuePublishingService);
    }

    @Test
    public void whenDeliverIsEnabledShouldAddMessageToDeliverQueue() {
        final Map<String, Object> content = newHashMap();
        content.put("col_name", "value");
        emailTargets.add(new EmailTarget("why@why.com", "twoTests?", content));
        when(yazinoConfiguration.getBoolean("emailvision.campaign.deliver")).thenReturn(true);
        when(campaignContentService.getEmailListName(CAMPAIGN_RUN_ID)).thenReturn("your mum");

        Map<NotificationChannelConfigType, String> channelConfig = new HashMap<>();
        channelConfig.put(NotificationChannelConfigType.TEMPLATE, "12345");
        channelConfig.put(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "ON");
        when(campaignContentService.getChannelConfig(CAMPAIGN_RUN_ID)).thenReturn(channelConfig);
        when(campaignCommanderClient.addEmailAddresses(emailTargets, CAMPAIGN_RUN_ID, "your mum")).thenReturn(666L);
        underTest.sendMessageToPlayers(campaignDeliverMessage);
        verify(queuePublishingService).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, 666L, "12345", "ON"));
    }
}
