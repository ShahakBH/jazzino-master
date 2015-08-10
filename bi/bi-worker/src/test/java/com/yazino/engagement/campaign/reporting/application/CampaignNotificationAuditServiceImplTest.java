package com.yazino.engagement.campaign.reporting.application;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CampaignNotificationAuditServiceImplTest {

    private static final Long CAMPAIGN_RUN_ID = 1234L;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    @Mock
    private QueuePublishingService<CampaignNotificationAuditMessage> campaignNotificationAuditPublishingService;

    private CampaignNotificationAuditService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignNotificationAuditServiceImpl(campaignNotificationAuditPublishingService);
    }

    @Test
    public void updateStatusMessageAboutToBeSentShouldCallPublisherWithStatusSendAttempt() {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();
        ArgumentCaptor<CampaignNotificationAuditMessage> argumentCaptor = ArgumentCaptor.forClass(CampaignNotificationAuditMessage.class);

        underTest.updateStatusMessageAboutToBeSent(pushNotificationMessage);

        verify(campaignNotificationAuditPublishingService).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getNotificationAuditType(), is(NotificationAuditType.SEND_ATTEMPT));
    }

    @Test
    public void updateStatusMessageSentSuccessfullyShouldCallPublisherWithStatusSendSuccessful() {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();
        ArgumentCaptor<CampaignNotificationAuditMessage> argumentCaptor = ArgumentCaptor.forClass(CampaignNotificationAuditMessage.class);

        underTest.updateStatusMessageSentSuccessfully(pushNotificationMessage);

        verify(campaignNotificationAuditPublishingService).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getNotificationAuditType(), is(NotificationAuditType.SEND_SUCCESSFUL));
    }

    @Test
    public void updateStatusMessageSentFailureShouldCallPublisherWithStatusSendFailure() {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();
        ArgumentCaptor<CampaignNotificationAuditMessage> argumentCaptor = ArgumentCaptor.forClass(CampaignNotificationAuditMessage.class);

        underTest.updateStatusMessageSentFailure(pushNotificationMessage);

        verify(campaignNotificationAuditPublishingService).send(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getNotificationAuditType(), is(NotificationAuditType.SEND_FAILURE));
    }

    @Test
    public void updateAuditStatusShouldNotBubbleUpExceptions() {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        doThrow(new RuntimeException()).when(campaignNotificationAuditPublishingService).send(Mockito.any(CampaignNotificationAuditMessage.class));

        underTest.updateStatusMessageSentFailure(pushNotificationMessage);

    }

    private PushNotificationMessage createPushNotificationMessage() {
        PlayerTarget playerTarget = new PlayerTarget("slots", "", PLAYER_ID, "", "", null);
        Map<String, String> messageContent = newHashMap();

        return new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);
    }
}
