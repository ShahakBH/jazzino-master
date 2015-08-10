package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.android.GoogleCloudMessagingForAndroidSender;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import org.joda.time.DateTimeConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotificationCampaignConsumerTest {

    private AndroidNotificationCampaignConsumer underTest;

    private NotificationCampaignConsumer abstractClassUnderTest;
    @Mock
    private GoogleCloudMessagingForAndroidSender androidSender;

    @Mock
    private CampaignNotificationAuditService campaignNotificationAuditService;

    @Before
    public void setUp() throws Exception {
        underTest = new AndroidNotificationCampaignConsumer(androidSender, campaignNotificationAuditService);
        abstractClassUnderTest = Mockito.mock(NotificationCampaignConsumer.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void handleMessageShouldCallSenderIfMessageIsValid() throws IOException {
        PushNotificationMessage pushNotificationMessage = createPushNotificationMessage();

        underTest.handle(pushNotificationMessage);

        verify(androidSender).sendRequest(pushNotificationMessage, 1234);
    }


    private PushNotificationMessage createPushNotificationMessage() {

        PlayerTarget playerTarget = new PlayerTarget("slots", "", BigDecimal.TEN, "1234ABCD", "", null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "my message Title");
        messageContent.put(DESCRIPTION.getKey(), "my message description");
        messageContent.put(MESSAGE.getKey(), "my message information");
        messageContent.put(TIME_TO_LIVE_IN_SECS.getKey(), "1234");

        return new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, 13578l);
    }

    @Test
    public void calculateTimeToLiveShouldReturnGivenTimeIfItIsValid() {
        PushNotificationMessage message = createPushNotificationMessage();
        message.getContent().put(TIME_TO_LIVE_IN_SECS.getKey(), "123");
        int timeToLive = underTest.calculateTimeToLive(message);

        assertThat(timeToLive, is(123));
    }

    @Test
    public void calculateTimeToLiveShouldReturnDefaultIfStringParsingFails() {
        PushNotificationMessage message = createPushNotificationMessage();
        message.getContent().put(TIME_TO_LIVE_IN_SECS.getKey(), "adhfh");
        int timeToLive = underTest.calculateTimeToLive(message);

        assertThat(timeToLive, is(DateTimeConstants.SECONDS_PER_DAY));
    }

    @Test
    public void calculateTimeToLiveShouldReturnDefaultIfTimeToLiveLessThanZero() {
        PushNotificationMessage message = createPushNotificationMessage();
        message.getContent().put(TIME_TO_LIVE_IN_SECS.getKey(), "-123");
        int timeToLive = underTest.calculateTimeToLive(message);

        assertThat(timeToLive, is(DateTimeConstants.SECONDS_PER_DAY));
    }


    @Test
    public void isValidFormatShouldReturnTrueIfReturnedStatusIsTrue() {
        PushNotificationMessage message = createPushNotificationMessage();
        assertThat(abstractClassUnderTest.isValidFormat(message), is(true));
    }

    @Test
    public void isValidFormatShouldReturnFalseIfReturnedStatusIsFalse() {
        PushNotificationMessage message = new PushNotificationMessage(null, null, null, 13578l);
        assertThat(abstractClassUnderTest.isValidFormat(message), is(false));
    }
}
