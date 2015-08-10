package com.yazino.engagement.campaign.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.dao.FacebookExclusionsDao;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import com.yazino.engagement.facebook.FacebookAppToUserRequestStatus;
import com.yazino.engagement.facebook.FacebookRequestSender;
import com.yazino.engagement.facebook.FacebookResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.facebook.FacebookDataContainer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import static com.yazino.engagement.ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION;
import static com.yazino.engagement.ChannelType.FACEBOOK_APP_TO_USER_REQUEST;
import static com.yazino.engagement.FacebookMessageType.APP_TO_USER_NOTIFICATION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FacebookNotificationCampaignConsumerTest {
    @Mock
    private FacebookRequestSender facebookRequestSender;

    @Mock
    private CampaignNotificationAuditService campaignNotificationAuditService;

    private ObjectMapper mapper = new ObjectMapper();

    private FacebookNotificationCampaignConsumer underTest;

    @Mock
    private CampaignNotificationDao campaignNotificationDao;

    @Mock
    private FacebookExclusionsDao facebookExclusionsDao;

    @Before
    public void setUp() throws Exception {
        underTest = new FacebookNotificationCampaignConsumer(facebookRequestSender, campaignNotificationAuditService,
                campaignNotificationDao, facebookExclusionsDao);
    }

    @Test
    public void handleShouldCallFacebookRequestSenderWithNotificationType() throws IOException {
        final PushNotificationMessage message = getPushNotificationMessage(FACEBOOK_APP_TO_USER_NOTIFICATION);
        underTest.handle(message);
        ArgumentCaptor<FacebookAppRequestEnvelope> argument = ArgumentCaptor.forClass(FacebookAppRequestEnvelope.class);
        verify(facebookRequestSender).sendRequest(eq(APP_TO_USER_NOTIFICATION), argument.capture());
        assertEquals("title", argument.getValue().getTitle());
        assertEquals("description", argument.getValue().getDescription());
        assertEquals("message", argument.getValue().getMessage());

        FacebookDataContainer expectedDataContainer = mapper.readValue(
                "{\"tracking\":{\"ref\":\"tracking\"},\"actions\":null,\"type\":\"Engagement\"}",
                FacebookDataContainer.class);

        assertEquals(expectedDataContainer, mapper.readValue(argument.getValue().getData(), FacebookDataContainer.class));
    }

    @Test
    public void handleShouldPutFBTrackingDataInCorrectFormat() throws IOException {

        PushNotificationMessage pushNotificationMessage = getPushNotificationMessage(FACEBOOK_APP_TO_USER_REQUEST);

        underTest.handle(pushNotificationMessage);

        ArgumentCaptor<FacebookAppRequestEnvelope> argument = ArgumentCaptor.forClass(FacebookAppRequestEnvelope.class);

        verify(facebookRequestSender).sendRequest(eq(FacebookMessageType.APP_TO_USER_REQUEST), argument.capture());
        assertEquals("title", argument.getValue().getTitle());
        assertEquals("description", argument.getValue().getDescription());
        assertEquals("message", argument.getValue().getMessage());

        FacebookDataContainer expectedDataContainer = mapper.readValue(
                "{\"tracking\":{\"ref\":\"tracking\"},\"actions\":null,\"type\":\"Engagement\"}",
                FacebookDataContainer.class);

        assertEquals(expectedDataContainer, mapper.readValue(argument.getValue().getData(), FacebookDataContainer.class));
    }

    @Test
    public void handleShouldCallFacebookSenderAndUpdateAuditInformation() {
        PushNotificationMessage pushNotificationMessage = getPushNotificationMessage(FACEBOOK_APP_TO_USER_REQUEST);

        when(facebookRequestSender.sendRequest(Mockito.any(FacebookMessageType.class),
                Mockito.any(FacebookAppRequestEnvelope.class))).thenReturn(
                new FacebookResponse(FacebookAppToUserRequestStatus.SENT, null));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentSuccessfully(pushNotificationMessage);
    }

    @Test
    public void handleShouldCallUpdateAuditInformationInCaseOfFacebookReturningUnSuccessfulResponse() {
        PushNotificationMessage pushNotificationMessage = getPushNotificationMessage(FACEBOOK_APP_TO_USER_REQUEST);

        when(facebookRequestSender.sendRequest(Mockito.any(FacebookMessageType.class),
                Mockito.any(FacebookAppRequestEnvelope.class))).thenReturn(
                new FacebookResponse(FacebookAppToUserRequestStatus.FAILED, null));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }

    @Test
    public void handleShouldUpdateAuditInformationInCaseOfException() {
        PushNotificationMessage pushNotificationMessage = getPushNotificationMessage(FACEBOOK_APP_TO_USER_REQUEST);

        when(facebookRequestSender.sendRequest(Mockito.any(FacebookMessageType.class), Mockito.any(FacebookAppRequestEnvelope.class)))
                .thenThrow(new RuntimeException("Exception from facebook"));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }

    @Test
    public void handleShouldLogFailureInFacebookExclusionsInCaseOfException() {
        PushNotificationMessage pushNotificationMessage = getPushNotificationMessage(FACEBOOK_APP_TO_USER_REQUEST);

        when(facebookRequestSender.sendRequest(Mockito.any(FacebookMessageType.class),
                Mockito.any(FacebookAppRequestEnvelope.class))).thenReturn(
                new FacebookResponse(FacebookAppToUserRequestStatus.FAILED, null));

        underTest.handle(pushNotificationMessage);

        verify(facebookExclusionsDao).logFailureInSendingFacebookNotification(
                pushNotificationMessage.getPlayerTarget().getPlayerId(),
                pushNotificationMessage.getPlayerTarget().getGameType());

    }


    private PushNotificationMessage getPushNotificationMessage(final ChannelType facebookAppToUserRequest) {
        final HashMap<String, String> content = new HashMap<>();
        content.put(MessageContentType.TITLE.getKey(), "title");
        content.put(MessageContentType.DESCRIPTION.getKey(), "description");
        content.put(MessageContentType.MESSAGE.getKey(), "message");
        content.put(MessageContentType.TRACKING.getKey(), "tracking");

        return new PushNotificationMessage(new PlayerTarget("SLOTS", "1asdqwd", BigDecimal.ONE, "targetToken",
                "bundle", null), content, facebookAppToUserRequest, 1l);
    }


}
