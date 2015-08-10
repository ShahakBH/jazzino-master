package com.yazino.engagement.amazon;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AmazonDeviceMessagingSendingServiceTest {
    public static final String CLIENT_SECRET = "whats da seclet";
    public static final String CLIENT_ID = "clientId";
    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final long CAMPAIGN_RUN_ID = 1l;
    public static final DateTime NOW = new DateTime(3000);

    @Mock
    AmazonAccessTokenService accessTokenService;
    @Mock
    YazinoConfiguration configuration;
    @Mock
    AmazonDeviceMessagingSender sender;
    @Mock
    CampaignNotificationAuditService auditService;
    @Mock
    MobileDeviceService mobileDeviceDao;

    AmazonDeviceMessagingSendingService underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        underTest = new AmazonDeviceMessagingSendingService(configuration, accessTokenService, sender, auditService, mobileDeviceDao);
        when(configuration.getString("amazon-device-messaging.client.secret")).thenReturn(CLIENT_SECRET);
        when(configuration.getString("amazon-device-messaging.client.id")).thenReturn(CLIENT_ID);
        when(accessTokenService.getAuthToken(CLIENT_ID, CLIENT_SECRET)).thenReturn(new AmazonAccessToken("this is the token", new DateTime().minusSeconds(10), "", ""));
    }

    @Test
    public void sendMessageShouldGetAccessTokenIfNull() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.sendMessage(message);
        verify(accessTokenService).getAuthToken(CLIENT_ID, CLIENT_SECRET);
    }

    @Test
    public void sendMessageShouldGetAccessTokenIBlank() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.setAccessToken(new AmazonAccessToken("", new DateTime().plusHours(1), "", ""));
        underTest.sendMessage(message);
        verify(accessTokenService).getAuthToken(CLIENT_ID, CLIENT_SECRET);
    }

    @Test
    public void sendMessageShouldGetAccessTokenIfAccessTokenIsPastExpireTime() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.setAccessToken(new AmazonAccessToken("this is the token", new DateTime().minusSeconds(10), "", ""));
        underTest.sendMessage(message);
        verify(accessTokenService).getAuthToken(CLIENT_ID, CLIENT_SECRET);
    }

    @Test
    public void sendMessageShouldSendMessageIfAccessTokenIsValid() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));

        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.SUCCESS)));
        verify(sender).sendMessage(message.getRegistrationId(),
                "valid access token",
                message.getTitle(),
                message.getTicker(),
                message.getMessage(),
                message.getCampaignRunId());
    }

    @Test
    public void sendMessageShouldAuditMessageOnSendSuccess() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));

        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.SUCCESS)));

        verify(auditService).updateAuditStatus(
                new CampaignNotificationAuditMessage(CAMPAIGN_RUN_ID,
                        PLAYER_ID,
                        ChannelType.AMAZON_DEVICE_MESSAGING,
                        "SLOTS",
                        NotificationAuditType.SEND_SUCCESSFUL, NOW));
    }

    @Test
    public void sendMessageShouldAuditSendAttempt() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);
        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));

        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.SUCCESS)));

        verify(auditService).updateAuditStatus(
                new CampaignNotificationAuditMessage(CAMPAIGN_RUN_ID,
                        PLAYER_ID,
                        ChannelType.AMAZON_DEVICE_MESSAGING,
                        "SLOTS",
                        NotificationAuditType.SEND_ATTEMPT, NOW));
    }

    @Test
    public void sendMessageShouldRefreshAccessTokenIfSendingReturns401() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);

        when(sender.sendMessage(message.getRegistrationId(),
                "valid access token",
                message.getTitle(),
                message.getTicker(),
                message.getMessage(),
                message.getCampaignRunId())).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "access token is expired"));

        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));
        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.RETRY)));
        verify(accessTokenService).getAuthToken(CLIENT_ID, CLIENT_SECRET);
    }

    @Test
    public void sendMessageShouldRetryIfSendingReturns500() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);

        when(sender.sendMessage(message.getRegistrationId(),
                "valid access token",
                message.getTitle(),
                message.getTicker(),
                message.getMessage(),
                message.getCampaignRunId())).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "access token is expired"));

        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));

        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.RETRY)));
    }

    @Test
    public void sendMessageShouldRetryIfSendingReturns503() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);

        when(sender.sendMessage(message.getRegistrationId(),
                "valid access token",
                message.getTitle(),
                message.getTicker(),
                message.getMessage(),
                message.getCampaignRunId())).thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, "access token is expired"));

        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));

        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.RETRY)));
    }

    @Test
    public void sendMessageShouldInvalidateUserIf400Error() throws Exception {
        AmazonDeviceMessage message = new AmazonDeviceMessage(PLAYER_ID, "registrationId", "title", "ticker", "message", "action", CAMPAIGN_RUN_ID);

        when(sender.sendMessage(message.getRegistrationId(),
                                "valid access token",
                                message.getTitle(),
                                message.getTicker(),
                                message.getMessage(),
                                message.getCampaignRunId())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Unregistered"));



        underTest.setAccessToken(new AmazonAccessToken("valid access token", new DateTime().plusHours(1), "", ""));
        assertThat(underTest.sendMessage(message), is(equalTo(AmazonSendStatus.FAILED)));
        verify(mobileDeviceDao).deregisterToken(Platform.AMAZON, message.getRegistrationId());
    }

}
