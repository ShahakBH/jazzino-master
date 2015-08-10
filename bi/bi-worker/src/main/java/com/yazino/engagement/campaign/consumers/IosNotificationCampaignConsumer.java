package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.mobile.yaps.message.PushMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.yaps.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("iosNotificationCampaignConsumer")
public class IosNotificationCampaignConsumer extends NotificationCampaignConsumer implements QueueMessageConsumer<PushNotificationMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(IosNotificationCampaignConsumer.class);
    public static final String NOTIFICATION_WAV = "notification.wav";
    public static final int BADGE = 1;

    private final Map<String, PushService> pushServiceMap;
    private final CampaignNotificationAuditService campaignNotificationAuditService;

    @Autowired
    public IosNotificationCampaignConsumer(PushMessageWorkerConfiguration pushMessageWorkerConfiguration,
                                           CampaignNotificationAuditService campaignNotificationAuditService) throws Exception {
        this.campaignNotificationAuditService = campaignNotificationAuditService;
        this.pushServiceMap = pushMessageWorkerConfiguration.pushServices();
    }

    @Override
    public void handle(PushNotificationMessage message) {
        LOG.debug("IosNotificationConsumer is attempting to consume a {}", message);
        try {
            if (isValidFormat(message)) {
                Map<String, String> messageContent = message.getContent();
                PlayerTarget playerTarget = message.getPlayerTarget();

                PushMessage pushMessage = new PushMessage(playerTarget.getGameType(), playerTarget.getPlayerId());
                pushMessage.setAlert(messageContent.get(MessageContentType.MESSAGE.getKey()));
                pushMessage.setBadge(BADGE);
                pushMessage.setSound(NOTIFICATION_WAV);

                int timeToLiveSinceEpoch = calculateTimeToLiveInSecondsSinceEpoch(message);
                pushMessage.setExpiryDateSecondsSinceEpoch(timeToLiveSinceEpoch);

                TargetedMessage targetedMessage = new TargetedMessage(playerTarget.getTargetToken(), pushMessage);

                campaignNotificationAuditService.updateStatusMessageAboutToBeSent(message);
                PushService pushService = pushServiceMap.get(playerTarget.getBundle());
                PushResponse response = pushService.pushMessage(targetedMessage);
                if (response != PushResponse.OK) {
                    LOG.debug("Ios sending failed for Message {} and returned response {}", message, response);
                    campaignNotificationAuditService.updateStatusMessageSentFailure(message);
                } else {
                    LOG.debug("Normal (none) response for message [{}]", message);
                    campaignNotificationAuditService.updateStatusMessageSentSuccessfully(message);
                }
            } else {
                LOG.error("Message is not valid: {}", message);
            }
        } catch (RecoverableException recoverable) {
            LOG.warn(" Sending notification to ios failed for {} throwing exception up to handler so it can be retried later",
                      message, recoverable);
            throw new RuntimeException("retry sending", recoverable);

        } catch (Exception e) {
            LOG.error(" Sending notification to ios failed for {} ", message, e);
            campaignNotificationAuditService.updateStatusMessageSentFailure(message);
        }
    }

    protected int calculateTimeToLiveInSecondsSinceEpoch(PushNotificationMessage message) {

        int timeToLiveInSeconds = calculateTimeToLive(message);

        DateTime expiryDate = new DateTime().plusSeconds(timeToLiveInSeconds);
        Long timeToLiveAsLong = expiryDate.getMillis() / DateTimeConstants.MILLIS_PER_SECOND;

        if (timeToLiveAsLong > Integer.MAX_VALUE || timeToLiveAsLong < Integer.MIN_VALUE) {
            LOG.error("Integer overflow of TimeToLiveInSecondsSinceEpoch : {}  for message {}", timeToLiveAsLong, message);
            return Integer.MAX_VALUE;
        } else {
            return timeToLiveAsLong.intValue();
        }
    }
}
