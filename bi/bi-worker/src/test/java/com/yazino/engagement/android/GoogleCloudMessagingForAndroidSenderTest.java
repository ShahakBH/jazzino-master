package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.android.gcm.server.ResultFactory.createResult;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.android.GoogleCloudMessagingForAndroidSender.*;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleCloudMessagingForAndroidSenderTest {

    private static final String REGISTRATION_ID = "REG_ID_1";
    private static final int RETRY_COUNT = 5;
    private static final String SAMPLE_MESSAGE = "sample message";
    private static final String EXTERNAL_ID = "1000";
    private static final String GAME_TYPE = "SAMPLE_GAME";
    private static final String MESSAGE_ID = "messageId";
    private static final String CANONICAL_REGISTRATION_ID = "canonicalRegistrationId";
    private static final String DEFAULT_MESSAGE_TYPE = "m";
    private static final String TITLE_VALUE = "title";
    private static final String DESCRIPTION_VALUE = "description";
    private static final int SECONDS_TO_LIVE = 60;
    private static final int APP_REQUEST_ID = 103;
    private static final String API_KEY = "api-key";
    private static final int FOUR_WEEKS_IN_SECONDS = 2419200;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final String DEVICE_TOKEN = "A1B2C3D4E5F";
    private static final String BUNDLE = "googleBundle";

    private Sender sender = Mockito.mock(Sender.class);
    private GoogleCloudMessagingForAndroidSender underTest;


    @Before
    public void setUp() throws IOException {
        when(sender.send(any(Message.class), anyString(), anyInt())).thenReturn(createResult(null, MESSAGE_ID, CANONICAL_REGISTRATION_ID));

        underTest = new GoogleCloudMessagingForAndroidSender(sender, API_KEY);
    }

    @Test
    public void shouldSendMessageObtainedFromSpecifiedEnvelope() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasMessage(SAMPLE_MESSAGE)), anyString(), anyInt());
    }

    @Test
    public void shouldSendMessageWithDefaultMessageType() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);

        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasMessageType(DEFAULT_MESSAGE_TYPE)), anyString(), anyInt());
    }

    @Test
    public void shouldSendToSpecifiedRegistrationId() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);

        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(any(Message.class), Matchers.eq(REGISTRATION_ID), anyInt());
    }

    @Test
    public void shouldSendWithRetryCountOfOne() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(any(Message.class), anyString(), Matchers.eq(RETRY_COUNT));
    }

    @Test
    public void shouldSendWithTimeToLive() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasTimeToLive(SECONDS_TO_LIVE)), anyString(), anyInt());

    }

    @Test
    public void shouldSendWithSpecifiedTitle() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasTitle(TITLE_VALUE)), anyString(), anyInt());

    }

    @Test
    public void shouldSendWithSpecifiedDescriptionAsTicker() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasTicker(DESCRIPTION_VALUE)), anyString(), anyInt());

    }

    @Test
    public void shouldSendWithAppRequestId() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasAppRequestId(APP_REQUEST_ID)), anyString(), anyInt());
    }

    @Test
    public void shouldSendAppRequestIdAsCollapseKey() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        verify(sender).send(argThat(hasCollapseKey(Integer.toString(APP_REQUEST_ID))), anyString(), anyInt());

    }

    @Test
    public void shouldReturnErrorCodeFromResult() throws IOException {
        String errorCode = "errorCode";
        when(sender.send(any(Message.class), anyString(), anyInt())).thenReturn(createResult(errorCode, MESSAGE_ID, CANONICAL_REGISTRATION_ID));
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);

        GoogleCloudMessagingResponse response = underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        assertEquals(errorCode, response.getErrorCode());
    }

    @Test
    public void shouldReturnMessageIdFromResult() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);

        GoogleCloudMessagingResponse response = underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        assertEquals(MESSAGE_ID, response.getMessageId());
    }

    @Test
    public void shouldReturnCanonicalRegistrationIdFromResult() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);

        GoogleCloudMessagingResponse response = underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, SECONDS_TO_LIVE);

        assertEquals(CANONICAL_REGISTRATION_ID, response.getCanonicalRegistrationId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeTimeToLiveValues() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTimeToLiveThatExceeds4Weeks() throws IOException {
        FacebookAppRequestEnvelope envelope = new FacebookAppRequestEnvelope(TITLE_VALUE, DESCRIPTION_VALUE, EXTERNAL_ID, GAME_TYPE, SAMPLE_MESSAGE, null, null);
        underTest.sendRequest(envelope, REGISTRATION_ID, APP_REQUEST_ID, FOUR_WEEKS_IN_SECONDS + 1);
    }

    @Test
    public void sendOfPushNotificationMessageShouldCallSenderWithAppropriateParameters() throws IOException {
        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "message Title");
        messageContent.put(DESCRIPTION.getKey(), "message description");
        messageContent.put(MESSAGE.getKey(), "message information");
        messageContent.put(CAMPAIGN_RUN_ID.getKey(), "12345");

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, 12345l);

        underTest.sendRequest(pushNotificationMessage, FOUR_WEEKS_IN_SECONDS);

        Message message = new Message.Builder()
                .addData(TITLE.getKey(), messageContent.get(TITLE.getKey()))
                .addData(MESSAGE.getKey(), messageContent.get(MESSAGE.getKey()))
                .addData(TYPE.getKey(), DEFAULT_MESSAGE_TYPE)
                .addData(TICKER_MESSAGE_KEY, messageContent.get(DESCRIPTION.getKey()))
                .addData(APP_REQUEST_KEY, messageContent.get(CAMPAIGN_RUN_ID.getKey()))
                .collapseKey(messageContent.get(CAMPAIGN_RUN_ID.getKey()))
                .timeToLive(FOUR_WEEKS_IN_SECONDS)
                .build();

        verify(sender).send(argThat(hasMessageObject(message)), eq(DEVICE_TOKEN), eq(DEFAULT_RETRY_COUNT));

    }

    private Matcher<Message> hasTitle(String expectedTitle) {
        return new WithTitleMatcher(expectedTitle);
    }

    private Matcher<Message> hasTicker(String expectedTicker) {
        return new WithTickerMatcher(expectedTicker);
    }

    private Matcher<Message> hasMessage(String expectedMessage) {
        return new WithMessageMatcher(expectedMessage);
    }

    private Matcher<Message> hasMessageType(String expectedMessageType) {
        return new WithMessageTypeMatcher(expectedMessageType);
    }

    private Matcher<Message> hasTimeToLive(int expectedTimeToLive) {
        return new WithTimeToLiveMatcher(expectedTimeToLive);
    }

    private Matcher<Message> hasAppRequestId(int appRequestId) {
        return new WithAppRequestIdMatcher(appRequestId);
    }

    private Matcher<Message> hasCollapseKey(String collapseKey) {
        return new WithCollapseKeyMatcher(collapseKey);
    }

    private Matcher<Message> hasMessageObject(Message message) {
        return new WithMessageObject(message);
    }
}
