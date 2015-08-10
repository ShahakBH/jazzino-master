package com.yazino.bi.operations.engagement.facebook;

import com.yazino.bi.operations.engagement.*;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.FacebookAppToUserMessage;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class FacebookAppRequestSenderTest {

    public static final int TARGET_ID1 = 12345;
    public static final int TARGET_ID2 = 23456;
    @Mock
    private EngagementCampaignDao appRequestDao;

    @Mock
    private QueuePublishingService<FacebookAppToUserMessage> queuePublishingService;

    private FacebookAppRequestPublisher underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new FacebookAppRequestPublisher(queuePublishingService, appRequestDao);
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfrequestIsNull() {
        underTest.sendRequest(null);
    }

    @Test
    public void shouldSendRequestForEachTargetInAppRequest() {
        AppRequestTarget target1 = new AppRequestTargetBuilder().withId(TARGET_ID1).build();
        AppRequestTarget target2 = new AppRequestTargetBuilder().withId(TARGET_ID2).build();
        List<AppRequestTarget> targets = Arrays.asList(target1, target2);

        EngagementCampaign engagementCampaign = new EngagementCampaignTestBuilder().build();
        when(appRequestDao.findAppRequestTargetsById(engagementCampaign.getId(), 0, Integer.MAX_VALUE)).thenReturn(targets);

        underTest.sendRequest(engagementCampaign);

        verify(queuePublishingService).send(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, engagementCampaign.getId(), TARGET_ID1));
        verify(queuePublishingService).send(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, engagementCampaign.getId(), TARGET_ID1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sendRequestShouldThrowIllegalArgumentExceptionForNonFacebookRequests() {
        AppRequestTarget target1 = new AppRequestTargetBuilder().withId(TARGET_ID1).build();
        List<AppRequestTarget> targets = Arrays.asList(target1);
        EngagementCampaign engagementCampaign = new EngagementCampaignTestBuilder().withChannelType(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID).build();
        when(appRequestDao.findAppRequestTargetsById(engagementCampaign.getId(), 0, Integer.MAX_VALUE)).thenReturn(targets);

        underTest.sendRequest(engagementCampaign);

        verify(queuePublishingService, never()).send(Matchers.<FacebookAppToUserMessage>any());
    }
}
