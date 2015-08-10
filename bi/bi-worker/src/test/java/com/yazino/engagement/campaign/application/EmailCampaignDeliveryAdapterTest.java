package com.yazino.engagement.campaign.application;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
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

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.engagement.ChannelType.EMAIL;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailCampaignDeliveryAdapterTest {

    public static final String EMAIL_LIST_NAME = "Campaign Title";
    public static final long CAMPAIGN_RUN_ID = 123L;
    public static final long UPLOAD_ID = 5445L;
    public static final String TEMPLATE_ID = "999";
    @Mock
    private CampaignCommanderClient campaignCommanderClient;
    @Mock
    private CampaignNotificationDao campaignNotificationDao;
    @Mock
    private CampaignContentService campaignContentService;

    private EmailCampaignUploaderAdapter underTest;
    private List<EmailTarget> targets = newArrayList();
    @Mock
    private QueuePublishingService<EmailCampaignDeliverMessage> emailCampaignDeliveryService;
    @Mock
    private YazinoConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        underTest = new EmailCampaignUploaderAdapter(campaignNotificationDao,
                campaignCommanderClient,
                campaignContentService,
                emailCampaignDeliveryService,
                configuration);
        when(configuration.getBoolean("emailvision.campaign.enabled", Boolean.FALSE)).thenReturn(true);
    }

    @Test
    public void sendMessageToPlayersShouldCallCampaignClient() {
        when(configuration.getBoolean("emailvision.campaign.deliver")).thenReturn(true);
        targets.add(new EmailTarget("bob@123.com", "bob", null));
        targets.add(new EmailTarget("jim@123.com", "jim", null));
        when(campaignNotificationDao.getEligibleEmailTargets(CAMPAIGN_RUN_ID)).thenReturn(targets);
        when(campaignContentService.getEmailListName(anyLong())).thenReturn(EMAIL_LIST_NAME);
        HashMap<NotificationChannelConfigType, String> channelConfig = new HashMap<NotificationChannelConfigType, String>();
        channelConfig.put(NotificationChannelConfigType.TEMPLATE, TEMPLATE_ID);
        channelConfig.put(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "ON");
        when(campaignContentService.getChannelConfig(CAMPAIGN_RUN_ID)).thenReturn(channelConfig);
        when(campaignCommanderClient.addEmailAddresses(targets, CAMPAIGN_RUN_ID, EMAIL_LIST_NAME)).thenReturn(UPLOAD_ID);

        underTest.sendMessageToPlayers(new CampaignDeliverMessage(CAMPAIGN_RUN_ID, EMAIL));

        verify(campaignCommanderClient).addEmailAddresses(targets, CAMPAIGN_RUN_ID, EMAIL_LIST_NAME);
        verify(emailCampaignDeliveryService).send(new EmailCampaignDeliverMessage(CAMPAIGN_RUN_ID, UPLOAD_ID, TEMPLATE_ID, "ON"));
    }

    @Test
    public void sendMesssageToPlayersShouldNotDoAnythingIfEmailHasNotBeenEnabled(){
        when(configuration.getBoolean("emailvision.campaign.enabled", Boolean.FALSE)).thenReturn(false);

        underTest.sendMessageToPlayers(new CampaignDeliverMessage(CAMPAIGN_RUN_ID, EMAIL));

        verifyNoMoreInteractions(campaignNotificationDao);
        verifyNoMoreInteractions(campaignCommanderClient);
        verifyNoMoreInteractions(emailCampaignDeliveryService);
    }

    @Test
    public void sendMessagesToPlayersShouldDoNothingIfNoPlayersAreSelected(){
        when(campaignNotificationDao.getEligibleEmailTargets(CAMPAIGN_RUN_ID)).thenReturn(targets);

        underTest.sendMessageToPlayers(new CampaignDeliverMessage(CAMPAIGN_RUN_ID, EMAIL));

        verifyNoMoreInteractions(campaignCommanderClient);
        verifyNoMoreInteractions(emailCampaignDeliveryService);

    }
}
