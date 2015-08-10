package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCampaignNotificationAuditService implements CampaignNotificationAuditService {

    private final Logger LOG = LoggerFactory.getLogger(AndroidNotificationCampaignConsumerIntegrationTest.class);

    @Override
    public void updateStatusMessageAboutToBeSent(PushNotificationMessage pushNotificationMessage) {
        LOG.debug("Message about to be sent {}", pushNotificationMessage);
    }

    @Override
    public void updateStatusMessageSentSuccessfully(PushNotificationMessage pushNotificationMessage) {
        LOG.debug("Message sending succeeded for {}", pushNotificationMessage);
    }

    @Override
    public void updateStatusMessageSentFailure(PushNotificationMessage pushNotificationMessage) {
        throw new RuntimeException("Intentionally throwing exception to indicate that message sending failed");
    }

    @Override
    public void updateStatusMessageFailureRetry(final PushNotificationMessage pushNotificationMessage) {

    }

    @Override
    public void updateAuditStatus(final CampaignNotificationAuditMessage auditMessage) {

    }
}
