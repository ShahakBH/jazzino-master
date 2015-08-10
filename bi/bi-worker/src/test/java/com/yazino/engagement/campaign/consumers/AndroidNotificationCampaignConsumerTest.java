package com.yazino.engagement.campaign.consumers;

import com.google.android.gcm.server.InvalidRequestException;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.android.GoogleCloudMessagingForAndroidSender;
import com.yazino.engagement.android.GoogleCloudMessagingResponse;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AndroidNotificationCampaignConsumerTest {

    public static final String TIME_TO_LIVE_VALUE = "1234";
    private AndroidNotificationCampaignConsumer underTest;

    @Mock
    private GoogleCloudMessagingForAndroidSender androidSender;

    @Mock
    private CampaignNotificationAuditService campaignNotificationAuditService;

    @Before
    public void setUp() throws Exception {
        underTest = new AndroidNotificationCampaignConsumer(androidSender, campaignNotificationAuditService);
    }

    @Test
    public void handleShouldCallAndroidSenderAndUpdateAuditInformation() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenReturn(new GoogleCloudMessagingResponse());
        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(androidSender).sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE));
        verify(campaignNotificationAuditService).updateStatusMessageSentSuccessfully(pushNotificationMessage);

    }

    @Test
    public void handleShouldUpdateAuditInformationInCaseOfGoogleReturningErrorCode() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        GoogleCloudMessagingResponse gcmResponse = new GoogleCloudMessagingResponse();
        gcmResponse.setErrorCode("Some Random Error Code");
        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenReturn(gcmResponse);

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);

    }

    @Test
    public void handleShouldUpdateAuditInformationInCaseOfException() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenThrow(new RuntimeException("Exception from Google"));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }

    @Test(expected = RuntimeException.class)
    public void handleShouldPutMessagesBackOnQueueIfWeReceiveAStatusBetween500To600() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenThrow(new InvalidRequestException(502, "Google service failure retry message"));
        underTest.handle(pushNotificationMessage);
    }

    @Test
    public void handleShouldSwallowExceptionIfStatusNotInThe500s() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenThrow(new InvalidRequestException(401, "Authorisation Failure do not retry"));


        try {
            underTest.handle(pushNotificationMessage);
        } catch (Exception e) {
            Assert.fail();
        }
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);

    }

    @Test(expected = RuntimeException.class)
    public void handleShouldLogRetryWithAuditService() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(androidSender.sendRequest(pushNotificationMessage, Integer.valueOf(TIME_TO_LIVE_VALUE))).thenThrow(new InvalidRequestException(505, "Google service failure retry message"));
        underTest.handle(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageFailureRetry(pushNotificationMessage);
    }
    private PushNotificationMessage createPushNotificationMessage() {
        PlayerTarget playerTarget = new PlayerTarget("slots", "", BigDecimal.TEN, "1234ABCD", "", null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "my message Title");
        messageContent.put(DESCRIPTION.getKey(), "my message description");
        messageContent.put(MESSAGE.getKey(), "my message information");
        messageContent.put(TIME_TO_LIVE_IN_SECS.getKey(), TIME_TO_LIVE_VALUE);

        return new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, 13578l);
    }
}
