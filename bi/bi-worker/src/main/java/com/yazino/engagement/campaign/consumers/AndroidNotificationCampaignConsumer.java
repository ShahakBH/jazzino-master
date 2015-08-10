package com.yazino.engagement.campaign.consumers;

import com.google.android.gcm.server.InvalidRequestException;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.android.GoogleCloudMessagingForAndroidSender;
import com.yazino.engagement.android.GoogleCloudMessagingResponse;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("androidNotificationCampaignConsumer")
public class AndroidNotificationCampaignConsumer extends NotificationCampaignConsumer implements QueueMessageConsumer<PushNotificationMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidNotificationCampaignConsumer.class);

    private final GoogleCloudMessagingForAndroidSender androidSender;
    private final CampaignNotificationAuditService campaignNotificationAuditService;

    @Autowired
    public AndroidNotificationCampaignConsumer(@Qualifier("googleCloudMessagingForAndroidSender") GoogleCloudMessagingForAndroidSender androidSender,
                                               CampaignNotificationAuditService campaignNotificationAuditService)
            throws Exception {
        this.androidSender = androidSender;
        this.campaignNotificationAuditService = campaignNotificationAuditService;
    }

    @Override
    public void handle(PushNotificationMessage message) {
        LOG.debug("Android consumer is attempting to consume a {}", message);
        try {
            if (isValidFormat(message)) {
                campaignNotificationAuditService.updateStatusMessageAboutToBeSent(message);
                GoogleCloudMessagingResponse googleCloudMessagingResponse = androidSender.sendRequest(message, calculateTimeToLive(message));
                if (googleCloudMessagingResponse != null && googleCloudMessagingResponse.getErrorCode() == null) {
                    campaignNotificationAuditService.updateStatusMessageSentSuccessfully(message);
                } else {
                    LOG.debug("Message sending failed for message: {} with response {}", message, googleCloudMessagingResponse);
                    campaignNotificationAuditService.updateStatusMessageSentFailure(message);
                }
            } else {
                LOG.error("Message is not valid: {}", message);
            }
        } catch (InvalidRequestException ire)   {
            if (ire.getHttpStatusCode() >= 500 && ire.getHttpStatusCode() < 600) {
                LOG.warn("send failed due to problems on Google Servers, putting message back onto queue for retry {}, {}", message, ire);
                campaignNotificationAuditService.updateStatusMessageFailureRetry(message);
                throw new RuntimeException("retry sending of message");
            }

            LOG.error(" Error while sending Android message {}", message, ire);
            campaignNotificationAuditService.updateStatusMessageSentFailure(message);

        } catch (Exception e) {
            LOG.error(" Error while sending Android message {}", message, e);
            campaignNotificationAuditService.updateStatusMessageSentFailure(message);
        }
    }

}
