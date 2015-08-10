package com.yazino.engagement.campaign.reporting.application;

import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.NotificationAuditType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Async
public class CampaignNotificationAuditServiceImpl implements CampaignNotificationAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignNotificationAuditServiceImpl.class);

    private final QueuePublishingService<CampaignNotificationAuditMessage> campaignNotificationAuditPublishingService;

    @Autowired
    public CampaignNotificationAuditServiceImpl(QueuePublishingService<CampaignNotificationAuditMessage> campaignNotificationAuditPublishingService) {
        this.campaignNotificationAuditPublishingService = campaignNotificationAuditPublishingService;
    }


    @Override
    public void updateStatusMessageAboutToBeSent(PushNotificationMessage pushNotificationMessage) {
        updateAuditStatus(pushNotificationMessage, NotificationAuditType.SEND_ATTEMPT);
    }

    @Override
    public void updateStatusMessageSentSuccessfully(PushNotificationMessage pushNotificationMessage) {
        updateAuditStatus(pushNotificationMessage, NotificationAuditType.SEND_SUCCESSFUL);
    }

    @Override
    public void updateStatusMessageSentFailure(PushNotificationMessage pushNotificationMessage) {
        updateAuditStatus(pushNotificationMessage, NotificationAuditType.SEND_FAILURE);
    }

    @Override
    public void updateStatusMessageFailureRetry(final PushNotificationMessage pushNotificationMessage) {
        updateAuditStatus(pushNotificationMessage, NotificationAuditType.SEND_FAILURE_RETRY);
    }

    public void updateAuditStatus(CampaignNotificationAuditMessage auditMessage)    {
        campaignNotificationAuditPublishingService.send(auditMessage);
    }

    private void updateAuditStatus(PushNotificationMessage pushNotificationMessage, NotificationAuditType notificationAuditType) {
        try {
            PlayerTarget playerTarget = pushNotificationMessage.getPlayerTarget();
            CampaignNotificationAuditMessage message = new CampaignNotificationAuditMessage(
                    pushNotificationMessage.getCampaignRunId(),
                    playerTarget.getPlayerId(),
                    pushNotificationMessage.getChannel(),
                    playerTarget.getGameType(),
                    notificationAuditType,
                    new DateTime());

            updateAuditStatus(message);
        } catch (Exception e) {
            LOG.error("Error while updating audit message {} for type {}", pushNotificationMessage, notificationAuditType);
        }
    }
}
