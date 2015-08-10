package com.yazino.bi.operations.engagement;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.yazino.bi.operations.engagement.facebook.FacebookAppRequestPublisher;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;


public class EngagementCampaignSenderTest {
    private EngagementCampaignSender underTest;

    @Mock
    private FacebookAppRequestPublisher facebookAppRequestPublisher;

    @Mock
    private EngagementCampaignDao dao;

    @Before
    public void init() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        MockitoAnnotations.initMocks(this);
        underTest = new EngagementCampaignSender(dao, facebookAppRequestPublisher);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void sendAppRequestShouldIgnoreRequestsBeingProcessed() {
        final EngagementCampaign engagementCampaignBeingProcessed = new EngagementCampaignBuilder().withStatus(EngagementCampaignStatus.PROCESSING)
                .build();

        final boolean sent = underTest.sendAppRequest(engagementCampaignBeingProcessed);

        assertFalse(sent);
    }

    @Test
    public void sendAppRequestShouldIgnoreSentRequests() {
        final EngagementCampaign sentEngagementCampaign = new EngagementCampaignBuilder().withStatus(EngagementCampaignStatus.SENT).build();

        final boolean sent = underTest.sendAppRequest(sentEngagementCampaign);

        assertFalse(sent);
    }

    @Test
    public void shouldSendFacebookAppRequest() {
        final EngagementCampaign request = new EngagementCampaignBuilder().withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST).withStatus(
                EngagementCampaignStatus.CREATED).build();

        final boolean sent = underTest.sendAppRequest(request);

        assertTrue(sent);
        verify(facebookAppRequestPublisher).sendRequest(request);
    }

    @Test
    public void shouldSendFacebookAppNotification() {
        final EngagementCampaign request = new EngagementCampaignBuilder().withChannelType(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION).withStatus(
                EngagementCampaignStatus.CREATED).build();

        final boolean sent = underTest.sendAppRequest(request);

        assertTrue(sent);
        verify(facebookAppRequestPublisher).sendRequest(request);
    }

    @Test
    public void testSendScheduledAppRequests() throws Exception {
        // given a couple of requests that are due
        final EngagementCampaign dueFBRequest1 = new EngagementCampaignBuilder()
                .withId(10).withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        final EngagementCampaign dueFBRequest2 = new EngagementCampaignBuilder()
                .withId(200)
                .withChannelType(ChannelType.FACEBOOK_APP_TO_USER_REQUEST)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        final EngagementCampaign dueFBNotification1 = new EngagementCampaignBuilder()
                .withId(10).withChannelType(ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION)
                .withStatus(EngagementCampaignStatus.CREATED)
                .build();
        Mockito.when(dao.findDueEngagementCampaigns(new DateTime())).thenReturn(Arrays.asList(
                dueFBRequest1, dueFBRequest2, dueFBNotification1));

        underTest.sendDueAppRequests();

        verify(facebookAppRequestPublisher).sendRequest(dueFBRequest1);
        verify(facebookAppRequestPublisher).sendRequest(dueFBRequest2);
        verify(facebookAppRequestPublisher).sendRequest(dueFBNotification1);
    }
}
