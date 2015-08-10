package com.yazino.engagement.android;

import com.yazino.engagement.GoogleCloudMessage;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;

@Component("googleCloudMessagingRequestProcessor")
public class GoogleCloudMessagingForAndroidRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudMessagingForAndroidRequestProcessor.class);

    public static final int DEFAULT_SECONDS_TO_LIVE = 60 * 60 * 24 * 2; // 2 DAYS
    public static final double MILLIS_PER_SECOND = 1000.0;

    private final GoogleCloudMessagingForAndroidSender sender;
    private final EngagementCampaignDao engagementCampaignDao;
    private final MobileDeviceService mobileDeviceDao;

    @Autowired
    public GoogleCloudMessagingForAndroidRequestProcessor(GoogleCloudMessagingForAndroidSender sender,
                                                          EngagementCampaignDao engagementCampaignDao,
                                                          MobileDeviceService mobileDeviceDao) {
        checkNotNull(sender);
        checkNotNull(engagementCampaignDao);
        checkNotNull(mobileDeviceDao);

        this.sender = sender;
        this.engagementCampaignDao = engagementCampaignDao;
        this.mobileDeviceDao = mobileDeviceDao;
    }

    public void process(GoogleCloudMessage message) throws IOException {
        LOG.debug("consuming message" + message);

        Integer appRequestTargetId = message.getAppRequestTargetId();
        FacebookAppRequestEnvelope envelope = engagementCampaignDao.fetchAppRequestEnvelopeByCampaignAndTargetId(message.getAppRequestId(), appRequestTargetId);
        if (envelope == null) {
            LOG.error("Could not load app request message for appRequestTargetId={}, appRequestId={}", appRequestTargetId, message.getAppRequestId());
            return;
        }

        String registrationId = message.getRegistrationId();
        int secondsToLive = calculateSecondsToExpiry(envelope.getExpires());
        if (secondsToLive < 0) {
            LOG.error("Ignoring app request message because it has already expired (appRequestTargetId={}, appRequestId={}, expiry={})", appRequestTargetId,
                    message.getAppRequestId(), envelope.getExpires());
            return;
        }

        GoogleCloudMessagingResponse response = sender.sendRequest(envelope, registrationId, message.getAppRequestId(), secondsToLive);
        updateRegistrationIdIfChanged(registrationId, response.getCanonicalRegistrationId(), message.getPlayerId());
        saveExternalReferenceIfSuccessful(appRequestTargetId, response);
        logMessageStatus(message, response);
    }

    private int calculateSecondsToExpiry(DateTime expires) {
        if (expires == null) {
            return DEFAULT_SECONDS_TO_LIVE;
        } else {
            DateTime now = new DateTime();
            return Seconds.secondsBetween(now, expires).getSeconds();
        }
    }

    private void saveExternalReferenceIfSuccessful(Integer targetId, GoogleCloudMessagingResponse response) {
        if (response.getMessageId() != null && response.getErrorCode() == null) {
            engagementCampaignDao.saveExternalReference(targetId, response.getMessageId());
        }
    }

    private void updateRegistrationIdIfChanged(String registrationId, String canonicalRegistrationId, BigDecimal playerId) {
        if (canonicalRegistrationId != null && !canonicalRegistrationId.equals(registrationId)) {
            mobileDeviceDao.replacePushTokenWith(playerId, Platform.ANDROID, registrationId, canonicalRegistrationId);
        }
    }

    private void logMessageStatus(final GoogleCloudMessage message,
                                  final GoogleCloudMessagingResponse response) {

        if (response.getErrorCode() == null) {
            LOG.debug("Google Cloud Message {} sent, response was {}", message, response);
        } else {
            LOG.debug("Unable to send Google Cloud Message {}, response was {}", message, response);
        }
    }
}
