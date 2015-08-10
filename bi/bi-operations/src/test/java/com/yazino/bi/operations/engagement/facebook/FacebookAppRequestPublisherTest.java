package com.yazino.bi.operations.engagement.facebook;

import com.yazino.bi.operations.engagement.*;
import com.yazino.engagement.*;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FacebookAppRequestPublisherTest {

    public static final int REQUEST_ID = 123;
    public static final String BINGO = "Bingo";
    public static final String SLOTS = "Slots";
    public static final BigDecimal PLAYER_ID1 = BigDecimal.valueOf(56);
    public static final BigDecimal PLAYER_ID2 = BigDecimal.valueOf(666);
    public static final String FACEBOOK_ID1 = "5565657";
    public static final String FACEBOOK_ID2 = "10101111";
    public static final int TARGET_ID1 = 34;
    public static final int TARGET_ID2 = 5666;

    @Mock
    QueuePublishingService<FacebookAppToUserMessage> queuePublishingService;

    @Mock
    private EngagementCampaignDao dao;

    private FacebookAppRequestPublisher underTest;

    private ChannelType channelType;
    private FacebookMessageType messageType;

    public FacebookAppRequestPublisherTest(ChannelType channelType, FacebookMessageType messageType) {
        this.channelType = channelType;
        this.messageType = messageType;
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new FacebookAppRequestPublisher(queuePublishingService, dao);
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenAppRequestIsNull(){
        underTest.sendRequest(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenNonFacebookChannel() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withChannelType(ChannelType.IOS)
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .build();

        underTest.sendRequest(request);
    }

    @Test
    public void shouldIgnoreSentAppRequest() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withChannelType(channelType)
                .withStatus(EngagementCampaignStatus.SENT)
                .build();

        underTest.sendRequest(request);

        verify(dao, never()).findAppRequestTargetsById(Mockito.<Integer>any(),Mockito.<Integer>any(), Mockito.<Integer>any());
        verify(queuePublishingService, never()).send(Mockito.<FacebookAppToUserMessage>any());
    }

    @Test
    public void shouldIgnoreProcessingAppRequest() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withChannelType(channelType)
                .withStatus(EngagementCampaignStatus.PROCESSING)
                .build();

        underTest.sendRequest(request);

        verify(dao, never()).findAppRequestTargetsById(Mockito.<Integer>any(),Mockito.<Integer>any(), Mockito.<Integer>any());
        verify(queuePublishingService, never()).send(Mockito.<FacebookAppToUserMessage>any());
    }

    @Ignore
    @Test
    public void shouldSetStateToProcessingBeforePublishingRequests() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withId(REQUEST_ID)
                .withChannelType(channelType)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        AppRequestTarget target1 = new AppRequestTargetBuilder().withAppRequestId(REQUEST_ID)
                .withGameType(SLOTS).withPlayerId(PLAYER_ID1).build();
        when(dao.findAppRequestTargetsById(REQUEST_ID, 0, Integer.MAX_VALUE)).thenReturn(Arrays.asList(target1));

        underTest.sendRequest(request);

        final EngagementCampaign expectedProcessingRequest = new EngagementCampaignBuilder(request).withStatus(EngagementCampaignStatus.PROCESSING).build();
        verify(dao).update(expectedProcessingRequest);
    }

    @Test
    public void shouldPublishFacebookMessages() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withId(REQUEST_ID)
                .withChannelType(channelType)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        AppRequestTarget target1 = new AppRequestTargetBuilder().withId(TARGET_ID1).withAppRequestId(REQUEST_ID)
                .withGameType(SLOTS).withPlayerId(PLAYER_ID1).withExternalId(FACEBOOK_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withId(TARGET_ID2).withAppRequestId(REQUEST_ID)
                .withGameType(BINGO).withPlayerId(PLAYER_ID2).withExternalId(FACEBOOK_ID2).build();
        when(dao.findAppRequestTargetsById(REQUEST_ID, 0, Integer.MAX_VALUE)).thenReturn(Arrays.asList(target1, target2));

        underTest.sendRequest(request);

        FacebookAppToUserMessage expectedPublishedMessage1 = new FacebookAppToUserMessage(messageType, REQUEST_ID, TARGET_ID1);
        FacebookAppToUserMessage expectedPublishedMessage2 = new FacebookAppToUserMessage(messageType, REQUEST_ID, TARGET_ID2);
        verify(queuePublishingService).send(expectedPublishedMessage1);
        verify(queuePublishingService).send(expectedPublishedMessage2);
    }

    @Test
    public void afterPublishingFacebookMessagesStateShouldBeSentAndSentDateShouldBeSetToNow() {
        final EngagementCampaign request = new EngagementCampaignBuilder()
                .withId(REQUEST_ID)
                .withChannelType(channelType)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        AppRequestTarget target1 = new AppRequestTargetBuilder().withAppRequestId(REQUEST_ID)
                .withGameType(SLOTS).withPlayerId(PLAYER_ID1).withExternalId(FACEBOOK_ID1).build();
        when(dao.findAppRequestTargetsById(REQUEST_ID, 0, Integer.MAX_VALUE)).thenReturn(Arrays.asList(target1));
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        underTest.sendRequest(request);

        final EngagementCampaign expectedSentRequest = new EngagementCampaignBuilder(request)
                .withStatus(EngagementCampaignStatus.SENT)
                .withSentDate(new DateTime()).build();
        verify(dao, times(2)).update(expectedSentRequest);

        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return asList(new Object[] {
                ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION, FacebookMessageType.APP_TO_USER_NOTIFICATION },
                new Object[] { ChannelType.FACEBOOK_APP_TO_USER_REQUEST, FacebookMessageType.APP_TO_USER_REQUEST });
    }


}
