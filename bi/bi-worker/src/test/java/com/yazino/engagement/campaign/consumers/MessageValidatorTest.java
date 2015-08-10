package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.consumers.MessageValidator.isValid;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageValidatorTest {

    public static final String MY_MESSAGE_INFORMATION = "my message information";
    public static final String TRACKING_DATA = "Tracking Data";
    public static final String GAME_TYPE = "SLOTS";
    public static final String BUNDLE = "com.yazino.YazinoApp";
    public static final long CAMPAIGN_RUN_ID = 13578l;
    private String DEVICE_TOKEN = "gjg1413gjg13g13jh";
    private String EXTERNAL_ID = "987654321";
    private BigDecimal PLAYER_ID = new BigDecimal("123456");

    private MessageValidatorStatus successStatus = new MessageValidatorStatus(true, "success");


    @Test
    public void isValidShouldReturnFailureIfMessageIsNull() {
        assertThat(MessageValidator.isValid(null), is(new MessageValidatorStatus(false, "Message is null")));
    }

    @Test
    public void isValidShouldReturnSuccessIfNeededValuesAreSetForAndroid() throws Exception {

        PushNotificationMessage pushNotificationMessage = createPushNotificationMessageForAndroid();
        assertThat(isValid(pushNotificationMessage), is(successStatus));

    }

    @Test
    public void isValidShouldReturnFailureIfPlayerTargetOrContentIsNullForAndroid() throws Exception {

        HashMap<String, String> contentMap = newHashMap();
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(null, contentMap,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Player Target or Content is null")));


        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage anotherPushNotificationMessage = new PushNotificationMessage(playerTarget, null,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);
        assertThat(isValid(anotherPushNotificationMessage), is(new MessageValidatorStatus(false, "Player Target or Content is null")));

    }

    @Test
    public void isValidShouldReturnFailureIfTitleIsNotSetForAndroid() throws Exception {

        PushNotificationMessage pushNotificationMessage = createPushNotificationMessageForAndroid();
        Map<String, String> contentMap = pushNotificationMessage.getContent();
        contentMap.put(TITLE.getKey(), null);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Title is not valid:null")));

    }

    @Test
    public void isValidShouldReturnFailureIfMessageIsNotSetForAndroid() throws Exception {

        PushNotificationMessage pushNotificationMessage = createPushNotificationMessageForAndroid();
        Map<String, String> contentMap = pushNotificationMessage.getContent();
        contentMap.put(MESSAGE.getKey(), null);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Message is not valid:null")));

    }

    @Test
    public void isValidShouldReturnFailureIfDescriptionIsNotSetForAndroid() throws Exception {

        PushNotificationMessage pushNotificationMessage = createPushNotificationMessageForAndroid();
        Map<String, String> contentMap = pushNotificationMessage.getContent();
        contentMap.put(DESCRIPTION.getKey(), null);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Description is not valid:null")));

    }

    private PushNotificationMessage createPushNotificationMessageForAndroid() {

        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "my message Title");
        messageContent.put(DESCRIPTION.getKey(), "my message description");
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        return new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);
    }

    @Test
    public void isValidShouldReturnFailureIfTargetTokenIsNotSetForAndroid() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, "", "", null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "my message Title");
        messageContent.put(DESCRIPTION.getKey(), "my message description");
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Registration Id is not valid:null")));

    }

    @Test
    public void isValidShouldReturnFalseIfChannelTypeIsNull() throws Exception {

        HashMap<String, String> contentMap = newHashMap();
        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                null, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Channel is null")));

    }

    @Test
    public void isValidShouldReturnSuccessIfChannelIsSetToFacebookRequestAndParametersAreCorrect() {
        HashMap<String, String> contentMap = newHashMap();
        contentMap.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);
        contentMap.put(TRACKING.getKey(), TRACKING_DATA);
        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(successStatus));
    }

    @Test
    public void isValidShouldReturnFailureIfChannelIsSetToFacebookRequestButGameTypeNotValid() {
        HashMap<String, String> contentMap = newHashMap();
        contentMap.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);
        contentMap.put(TRACKING.getKey(), TRACKING_DATA);
        PlayerTarget playerTarget = new PlayerTarget(null, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "GameType is not valid:null")));
    }

    @Test
    public void isValidShouldReturnFailureIfChannelIsSetToFacebookRequestButExternalIdMissing() {
        HashMap<String, String> contentMap = newHashMap();
        contentMap.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);
        contentMap.put(TRACKING.getKey(), TRACKING_DATA);
        PlayerTarget playerTarget = new PlayerTarget("slots", null, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "External Id is not valid:null")));
    }

    @Test
    public void isValidShouldReturnFailureIfChannelIsSetToFacebookRequestButMessageIsMissing() {
        HashMap<String, String> contentMap = newHashMap();
        contentMap.put(TRACKING.getKey(), TRACKING_DATA);
        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Message is not valid:null")));
    }

    @Test
    public void isValidShouldReturnFailureIfChannelIsSetToFacebookRequestButTrackingDataIsMissing() {
        HashMap<String, String> contentMap = newHashMap();
        contentMap.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);
        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, "", null);
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, CAMPAIGN_RUN_ID);
        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Data is not valid:null")));
    }

    @Test
    public void isValidShouldReturnSuccessIfNeededValuesAreSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(successStatus));

    }

    @Test
    public void isValidShouldReturnFailureIfPlayerIdNotSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, null, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "PlayerId is not valid :null")));

    }

    @Test
    public void isValidShouldReturnFailureIfGameTypeNotSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(null, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "GameType is not valid :null")));

    }

    @Test
    public void isValidShouldReturnFailureIfDeviceTokenNotSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, PLAYER_ID, null, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Device Token is not valid :null")));

    }

    @Test
    public void isValidShouldReturnFailureIfBundleNotSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, null, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), MY_MESSAGE_INFORMATION);

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Bundle is not valid :null")));

    }

    @Test
    public void isValidShouldReturnFailureIfMessageNotSetForIos() throws Exception {

        PlayerTarget playerTarget = new PlayerTarget(GAME_TYPE, EXTERNAL_ID, PLAYER_ID, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> messageContent = newHashMap();

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent, ChannelType.IOS, CAMPAIGN_RUN_ID);

        assertThat(isValid(pushNotificationMessage), is(new MessageValidatorStatus(false, "Message is not valid :null")));

    }


}
