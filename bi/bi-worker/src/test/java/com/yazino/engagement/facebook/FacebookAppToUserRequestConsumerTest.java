package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.FacebookAppToUserMessage;
import com.yazino.engagement.FacebookMessageType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class FacebookAppToUserRequestConsumerTest {
    public static final int APP_REQUEST_ID = 5678;
    public static final String FACEBOOK_ID = "101";
    public static final int TARGET_ID = 101;
    public static final String MESSAGE = "This is a message";
    public static final String TRACKING_DATA = "tracking data";
    public static final String SLOTS = "SLOTS";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private FacebookAppToUserRequestConsumer underTest;

    @Mock
    private FacebookRequestSender facebookRequestSender;

    @Mock
    private EngagementCampaignDao appRequestConsumerDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(facebookRequestSender.sendRequest(Matchers.eq(FacebookMessageType.APP_TO_USER_REQUEST), Matchers.any(FacebookAppRequestEnvelope.class))).thenReturn(new
                FacebookResponse(FacebookAppToUserRequestStatus.SENT, "123"));
        when(facebookRequestSender.sendRequest(Matchers.eq(FacebookMessageType.APP_TO_USER_NOTIFICATION), Matchers.any(FacebookAppRequestEnvelope.class))).thenReturn(new
                FacebookResponse(FacebookAppToUserRequestStatus.SENT, null));
        underTest = new FacebookAppToUserRequestConsumer(facebookRequestSender, appRequestConsumerDao);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfSenderIsNull() {
        new FacebookAppToUserRequestConsumer(null, appRequestConsumerDao);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfDaoIsNull() {
        new FacebookAppToUserRequestConsumer(facebookRequestSender, null);
    }

    @Test
    public void handleShouldFetchMessageDetailsFromDao() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));


        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));
        verify(appRequestConsumerDao).fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID);
    }

    @Test
    public void handleShouldSendFacebookAppRequestForTarget() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));

        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));

        verify(facebookRequestSender).sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null));
    }

    @Test
    public void handleShouldNotSendFacebookAppRequestForNULLGameType() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, "NULL", MESSAGE, TRACKING_DATA, null));

        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));

        verifyZeroInteractions(facebookRequestSender);
    }

    @Test
    public void handlePropagateFacebookMessageTypeWhenSending() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));

        FacebookAppToUserMessage message = new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID);
        underTest.handle(message);

        verify(facebookRequestSender).sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null));

        message = new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_NOTIFICATION, APP_REQUEST_ID, TARGET_ID);
        underTest.handle(message);

        verify(facebookRequestSender).sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null));
    }

    @Test
    public void handleShouldSaveFacebookRequestIdIfMessageSent() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));
        when(facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null))).thenReturn(new FacebookResponse(FacebookAppToUserRequestStatus.SENT, "aRequestId"));
        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));
        verify(appRequestConsumerDao).saveExternalReference(TARGET_ID, "aRequestId");
    }

    @Test
    public void handleShouldNotSaveExternalReferenceWhenPublishingNotification() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));
        when(facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null))).thenReturn(new FacebookResponse(FacebookAppToUserRequestStatus.SENT, null));
        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_NOTIFICATION, APP_REQUEST_ID, TARGET_ID));
        verify(appRequestConsumerDao, never()).saveExternalReference(TARGET_ID, null);
    }

    @Test
    public void handleShouldNotSaveFacebookRequestIdIfMessageNotSent() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE, TRACKING_DATA, null));
        when(facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null))).thenReturn(new FacebookResponse(FacebookAppToUserRequestStatus.FAILED, null));
        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));
        verify(appRequestConsumerDao, never()).saveExternalReference(TARGET_ID, "aRequestId");
    }

    @Test
    public void handleShouldLogWarningWhenAppRequestMessageCouldNotBeLoaded() {
        when(appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, TARGET_ID))
                .thenReturn(null);

//        ListAppender listAppender = ListAppender.addTo(FacebookAppToUserRequestConsumer.class);

        underTest.handle(new FacebookAppToUserMessage(FacebookMessageType.APP_TO_USER_REQUEST, APP_REQUEST_ID, TARGET_ID));

//        assertTrue(listAppender.getMessages().contains("Could not load facebook app request message for targetId=" + TARGET_ID + ", appRequestId=" + APP_REQUEST_ID));  // TODO fix class cast exception
        verify(facebookRequestSender, never()).sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_ID, SLOTS, MESSAGE,
                TRACKING_DATA, null));
    }
}
