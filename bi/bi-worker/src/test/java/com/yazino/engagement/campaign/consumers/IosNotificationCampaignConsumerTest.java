package com.yazino.engagement.campaign.consumers;

import com.rabbitmq.client.AMQP;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.mobile.yaps.message.PushMessage;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.yaps.*;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.MESSAGE;
import static com.yazino.engagement.campaign.domain.MessageContentType.TIME_TO_LIVE_IN_SECS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IosNotificationCampaignConsumerTest {

    public static final String BUNDLE = "bundle";
    public static final String DEVICE_TOKEN = "1A2B3C4D";
    public static final int BADGE_VALUE = 1;
    public static final String NOTIFICATION_WAV = "notification.wav";
    private IosNotificationCampaignConsumer underTest;
    @Mock
    private PushService pushService;
    @Mock
    private PushMessageWorkerConfiguration pushMessageWorkerConfiguration;

    @Mock
    private CampaignNotificationAuditService campaignNotificationAuditService;

    private DateTime currentDateTime = new DateTime(2013, 6, 28, 11, 30);
    public static final String MESSAGE_VALUE = "this is a wonderful messsage";

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(currentDateTime.getMillis());
        Map<String, PushService> pushServiceMap = newHashMap();
        pushServiceMap.put(BUNDLE, pushService);
        when(pushMessageWorkerConfiguration.pushServices()).thenReturn(pushServiceMap);
        underTest = new IosNotificationCampaignConsumer(pushMessageWorkerConfiguration, campaignNotificationAuditService);

    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void handleMessageShouldSendMessageToPushServiceAndUpdateAuditInformation() throws Exception {

        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(pushService.pushMessage(any(TargetedMessage.class))).thenReturn(PushResponse.OK);

        underTest.handle(pushNotificationMessage);


        PlayerTarget playerTarget = pushNotificationMessage.getPlayerTarget();
        PushMessage pushMessage = new PushMessage(playerTarget.getGameType(), playerTarget.getPlayerId());
        pushMessage.setAlert(MESSAGE_VALUE);
        pushMessage.setBadge(BADGE_VALUE);
        pushMessage.setSound(NOTIFICATION_WAV);
        Long timeToLiveInSecondsSinceEpoch = currentDateTime.plusSeconds(DateTimeConstants.SECONDS_PER_HOUR).getMillis() / DateTimeConstants.MILLIS_PER_SECOND;
        pushMessage.setExpiryDateSecondsSinceEpoch(timeToLiveInSecondsSinceEpoch.intValue());
        TargetedMessage targetedMessage = new TargetedMessage(playerTarget.getTargetToken(), pushMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(pushService).pushMessage(targetedMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentSuccessfully(pushNotificationMessage);
    }

    private PushNotificationMessage createPushNotificationMessage() {
        PlayerTarget playerTarget = new PlayerTarget("SLOTS", "1234567", BigDecimal.TEN, DEVICE_TOKEN, BUNDLE, null);
        Map<String, String> contentMap = newHashMap();
        contentMap.put(MESSAGE.getKey(), MESSAGE_VALUE);
        contentMap.put(TIME_TO_LIVE_IN_SECS.getKey(), Integer.toString(DateTimeConstants.SECONDS_PER_HOUR));
        return new PushNotificationMessage(playerTarget, contentMap, ChannelType.IOS, 1234l);
    }

    @Test
    public void handleMessageShouldUpdateAuditInformationInCaseOfException() throws Exception {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(pushService.pushMessage(any(TargetedMessage.class))).thenThrow(new RuntimeException("Push Message Exception"));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }

    @Test
    public void handleMessageShouldUpdateAuditInformationInCaseOfAppleResponseSayingUnSuccessful() throws Exception {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        when(pushService.pushMessage(any(TargetedMessage.class))).thenReturn(new PushResponse(AppleResponseCode.InvalidToken, null));

        underTest.handle(pushNotificationMessage);

        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }


    @Test
    public void calculateTimeToLiveInSecondsSinceEpochShouldReturnCorrectValueIfThereIsNoOverflow() {

        PlayerTarget playerTarget = new PlayerTarget("SLOTS", "1234567", BigDecimal.TEN, "", BUNDLE, null);
        Map<String, String> contentMap = newHashMap();
        contentMap.put(TIME_TO_LIVE_IN_SECS.getKey(), Integer.toString(DateTimeConstants.SECONDS_PER_HOUR));
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap, ChannelType.IOS, 1234l);

        int returnedTimeInSecondsSinceEpoch = underTest.calculateTimeToLiveInSecondsSinceEpoch(pushNotificationMessage);

        Long expectedTimeInSecondsSinceEpoch = currentDateTime.plusSeconds(DateTimeConstants.SECONDS_PER_HOUR).getMillis() / DateTimeConstants.MILLIS_PER_SECOND;

        assertThat(returnedTimeInSecondsSinceEpoch, is(expectedTimeInSecondsSinceEpoch.intValue()));

    }

    @Test
    public void calculateTimeToLiveInSecondsSinceEpochShouldReturnIntegerMaxValueIfThereIsAnOverflow() {

        PlayerTarget playerTarget = new PlayerTarget("SLOTS", "1234567", BigDecimal.TEN, "", BUNDLE, null);
        Map<String, String> contentMap = newHashMap();
        contentMap.put(TIME_TO_LIVE_IN_SECS.getKey(), Integer.toString(Integer.MAX_VALUE));
        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, contentMap, ChannelType.IOS, 123456l);

        int returnedTimeInSecondsSinceEpoch = underTest.calculateTimeToLiveInSecondsSinceEpoch(pushNotificationMessage);

        assertThat(returnedTimeInSecondsSinceEpoch, is(Integer.MAX_VALUE));

    }

    @Test(expected = RuntimeException.class)
    public void ioRecoverableFailureShouldThrowExceptionUpToHandler() throws Exception {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();
        when(pushService.pushMessage(any(TargetedMessage.class))).thenThrow(RecoverableException.class);

        underTest.handle(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageAboutToBeSent(pushNotificationMessage);
        verifyNoMoreInteractions();
    }

    @Test
    public void nonRecoverableFailureShouldNotThrowExceptionUpToHandler() throws Exception {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();
        when(pushService.pushMessage(any(TargetedMessage.class))).thenThrow(Exception.class);

        underTest.handle(pushNotificationMessage);
        verify(campaignNotificationAuditService).updateStatusMessageSentFailure(pushNotificationMessage);
    }

}
