package com.yazino.engagement.amazon;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import org.apache.commons.httpclient.HttpStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class AmazonDeviceMessagingSendingService {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonDeviceMessagingSendingService.class);
    private final YazinoConfiguration configuration;
    private final AmazonAccessTokenService accessTokenService;
    private final AmazonDeviceMessagingSender sender;
    private final CampaignNotificationAuditService auditService;
    private final MobileDeviceService mobileDeviceDao;
    private AmazonAccessToken accessToken;

    @Autowired
    public AmazonDeviceMessagingSendingService(final YazinoConfiguration configuration,
                                               final AmazonAccessTokenService accessTokenService,
                                               final AmazonDeviceMessagingSender sender,
                                               final CampaignNotificationAuditService auditService,
                                               final MobileDeviceService mobileDeviceDao) {

        this.configuration = configuration;
        this.accessTokenService = accessTokenService;
        this.sender = sender;
        this.auditService = auditService;
        this.mobileDeviceDao = mobileDeviceDao;
    }

    public AmazonSendStatus sendMessage(final AmazonDeviceMessage message) throws Exception {

        if (accessTokenIsInvalid()) {
            try {
                fetchAmazonToken();
            } catch (Exception e) {
                LOG.error("problem with getting amazon access token {}", e);
                throw new RuntimeException("Could not get access token for amazon, messages cannot be sent at this time");
            }
        }

        try {
            LOG.debug("attempting to send ADM message through amazon");
            auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_ATTEMPT));
            sender.sendMessage(message.getRegistrationId(),
                               accessToken.getToken(),
                               message.getTitle(),
                               message.getTicker(),
                               message.getMessage(),
                               message.getCampaignRunId());
        } catch (HttpClientErrorException e) {
            int statusCode = e.getStatusCode().value();
            switch (statusCode) {
                case HttpStatus.SC_BAD_REQUEST:
                    LOG.warn("problems sending ADM to {} :{}, {}", message, e.getStatusCode(), e.getResponseBodyAsString());
                    // if unregistered we should remove from system
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_FAILURE));
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.UNREGISTERED));
                    mobileDeviceDao.deregisterToken(Platform.AMAZON, message.getRegistrationId());
                    return AmazonSendStatus.FAILED;
                case HttpStatus.SC_UNAUTHORIZED:
                    LOG.warn("access token expired, fetching new access token");
                    fetchAmazonToken();
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_FAILURE_RETRY));
                    return AmazonSendStatus.RETRY;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_FAILURE_RETRY));
                    return AmazonSendStatus.RETRY;
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_FAILURE_RETRY));
                    return AmazonSendStatus.RETRY;
                default:
                    LOG.warn("sending of amazon device messaging failed:{}, {}", statusCode, e.getResponseBodyAsString());
                    auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_FAILURE));
                    return AmazonSendStatus.FAILED;
            }
        }

        auditService.updateAuditStatus(convertMessage(message, NotificationAuditType.SEND_SUCCESSFUL));
        return AmazonSendStatus.SUCCESS;
    }

    private CampaignNotificationAuditMessage convertMessage(final AmazonDeviceMessage message, final NotificationAuditType sendSuccessful) {
        return new CampaignNotificationAuditMessage(message.getCampaignRunId(),
                                                    message.getPlayerId(),
                                                    ChannelType.AMAZON_DEVICE_MESSAGING,
                                                    "SLOTS",
                                                    sendSuccessful,
                                                    new DateTime());
    }

    private boolean accessTokenIsInvalid() {
        return null == accessToken || isBlank(accessToken.getToken()) || accessToken.getExpireTime().isBeforeNow();
    }

    private void fetchAmazonToken() throws Exception {
        accessToken = accessTokenService.getAuthToken(
                configuration.getString("amazon-device-messaging.client.id"),
                configuration.getString("amazon-device-messaging.client.secret"));
    }

    public void setAccessToken(final AmazonAccessToken accessToken) {
        this.accessToken = accessToken;
    }
}
