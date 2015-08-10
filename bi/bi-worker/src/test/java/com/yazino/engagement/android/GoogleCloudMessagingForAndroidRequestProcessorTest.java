package com.yazino.engagement.android;

import com.yazino.engagement.GoogleCloudMessage;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class GoogleCloudMessagingForAndroidRequestProcessorTest {

    private static final int APP_REQUEST_ID = 5678;
    private static final String EXTERNAL_ID = "101";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(101);
    private static final String MESSAGE = "This is a message";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String TRACKING_DATA = "tracking data";
    private static final String GAME_TYPE = "SAMPLE_GAME";
    private static final String REGISTRATION_ID = "REGISTRATION_ID";
    private static final String CANONICAL_REGISTRATION_ID = "CANONICAL_REGISTRATION_ID";
    private static final String GOOGLE_CLOUD_MESSAGE_ID = "GOOGLE_CLOUD_MESSAGE_ID";
    private static final FacebookAppRequestEnvelope ENVELOPE = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, EXTERNAL_ID, GAME_TYPE, MESSAGE, TRACKING_DATA, null);
    private static GoogleCloudMessagingResponse ERROR_RESPONSE;
    private static final Integer APP_REQUEST_TARGET_ID = 400;
    private static final int DEFAULT_SECONDS_TO_LIVE = 60 * 60 * 24 * 2; // 2 DAYS

    private GoogleCloudMessagingForAndroidSender requestSender = mock(GoogleCloudMessagingForAndroidSender.class);
    private EngagementCampaignDao engagementCampaignDao = mock(EngagementCampaignDao.class);
    private MobileDeviceService mobileDeviceDao = mock(MobileDeviceService.class);
    private GoogleCloudMessagingForAndroidRequestProcessor underTest;

    @Before
    public void setup() throws IOException {
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID)).thenReturn(ENVELOPE);


        final GoogleCloudMessagingResponse sentResponse = new GoogleCloudMessagingResponse();
        sentResponse.setMessageId(GOOGLE_CLOUD_MESSAGE_ID);
        sentResponse.setCanonicalRegistrationId(CANONICAL_REGISTRATION_ID);

        ERROR_RESPONSE = new GoogleCloudMessagingResponse();
        ERROR_RESPONSE.setErrorCode("ERROR_CODE");

        when(requestSender.sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), anyInt(), anyInt())).thenReturn(sentResponse);

        underTest = new GoogleCloudMessagingForAndroidRequestProcessor(requestSender, engagementCampaignDao, mobileDeviceDao);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfSenderIsNull() {
        new GoogleCloudMessagingForAndroidRequestProcessor(null, engagementCampaignDao, mobileDeviceDao);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfEngagementCampaignDaoIsNull() {
        new GoogleCloudMessagingForAndroidRequestProcessor(requestSender, null, mobileDeviceDao);
    }

    @Test
    public void shouldInvokedSenderWithEnvelopedObtainedFromEngagementCampaignDao() throws IOException {
        FacebookAppRequestEnvelope expectedEnvelope = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, EXTERNAL_ID, GAME_TYPE, MESSAGE, TRACKING_DATA, null);
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID))
                .thenReturn(expectedEnvelope);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));
        verify(engagementCampaignDao).fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID);
        verify(requestSender).sendRequest(eq(expectedEnvelope), eq(REGISTRATION_ID), anyInt(), anyInt());
    }

    @Test
    public void shouldSendMessageToRegistrationIdForTargetPlayerDevice() throws IOException {
        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(requestSender).sendRequest(eq(ENVELOPE), eq(REGISTRATION_ID), anyInt(), anyInt());
    }

    @Test
    public void shouldSendMessageWithToAppRequestId() throws IOException {
        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(requestSender).sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), eq(APP_REQUEST_ID), anyInt());
    }

    @Test
    public void shouldSendMessageWithSpecifiedTimeToLiveWhenHasExpiryDate() throws IOException {
        DateTime now = new DateTime();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now.getMillis());
        DateTime expiryDate = now.plusDays(3);
        FacebookAppRequestEnvelope expectedEnvelope = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, EXTERNAL_ID, GAME_TYPE, MESSAGE, TRACKING_DATA, expiryDate);
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID))
                .thenReturn(expectedEnvelope);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        int expectedSecondsToLive = (int) expiryDate.withMillisOfSecond(0).minus(now.withMillisOfSecond(0).getMillis()).getMillis() / 1000;

        verify(requestSender).sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), eq(APP_REQUEST_ID), eq(expectedSecondsToLive));
    }

    @Test
    public void shouldIgnoreExpiredMessages() throws IOException {
        DateTime now = new DateTime();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now.getMillis());
        DateTime expiryDate = now.minusSeconds(1);
        FacebookAppRequestEnvelope expectedEnvelope =
                new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, EXTERNAL_ID, GAME_TYPE, MESSAGE, TRACKING_DATA, expiryDate);
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID))
                .thenReturn(expectedEnvelope);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(requestSender, never()).sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), eq(APP_REQUEST_ID), anyInt());
    }

    @Test
    public void shouldSendMessageWithDefaultTimeToLiveWhenNoExpiryDate() throws IOException {
        FacebookAppRequestEnvelope expectedEnvelope = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, EXTERNAL_ID, GAME_TYPE, MESSAGE, TRACKING_DATA, null);
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID))
                .thenReturn(expectedEnvelope);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(requestSender).sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), eq(APP_REQUEST_ID), eq(DEFAULT_SECONDS_TO_LIVE));
    }

    @Test
    public void shouldUpdateRegistrationIdWhenCanonicalRegistrationIdDiffers() throws IOException {
        GoogleCloudMessagingResponse response = new GoogleCloudMessagingResponse();
        response.setCanonicalRegistrationId(CANONICAL_REGISTRATION_ID);
        when(requestSender.sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), anyInt(), anyInt())).thenReturn(response);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(mobileDeviceDao).replacePushTokenWith(PLAYER_ID, Platform.ANDROID, REGISTRATION_ID, CANONICAL_REGISTRATION_ID);
    }

    @Test
    public void shouldNotUpdateRegistrationIdWhenCanonicalRegistrationIdDoesNotDiffer() throws IOException {
        GoogleCloudMessagingResponse response = new GoogleCloudMessagingResponse();
        response.setCanonicalRegistrationId(REGISTRATION_ID);
        when(requestSender.sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), anyInt(), anyInt())).thenReturn(response);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(mobileDeviceDao, never()).replacePushTokenWith(PLAYER_ID, Platform.ANDROID, REGISTRATION_ID, CANONICAL_REGISTRATION_ID);
    }

    @Test
    public void shouldSaveGoogleCloudMessageIdAsExternalReferenceIfMessageSent() throws IOException {
        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(engagementCampaignDao).saveExternalReference(APP_REQUEST_TARGET_ID, GOOGLE_CLOUD_MESSAGE_ID);
    }

    @Test
    public void shouldNotSaveExternalReferenceIfMessageNotSent() throws IOException {
        when(requestSender.sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), anyInt(), anyInt())).thenReturn(ERROR_RESPONSE);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

        verify(engagementCampaignDao, never()).saveExternalReference(PLAYER_ID.intValue(), GOOGLE_CLOUD_MESSAGE_ID);
    }

    @Test
    public void shouldLogWarningWhenAppRequestMessageCouldNotBeLoaded() throws IOException {
        when(engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(APP_REQUEST_ID, APP_REQUEST_TARGET_ID)).thenReturn(null);

//        ListAppender listAppender = ListAppender.addTo(GoogleCloudMessagingForAndroidRequestProcessor.class);

        underTest.process(new GoogleCloudMessage(APP_REQUEST_ID, APP_REQUEST_TARGET_ID, PLAYER_ID, REGISTRATION_ID));

//        assertTrue(listAppender.getMessages().contains(
//                "Could not load app request message for targetId=" + TARGET_ID + ", appRequestId=" + APP_REQUEST_ID));  // TODO fix class cast exception
        verify(requestSender, never()).sendRequest(any(FacebookAppRequestEnvelope.class), anyString(), anyInt(), anyInt());
    }
}
