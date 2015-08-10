package com.yazino.engagement.campaign.reporting.application;

import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;

public interface CampaignNotificationAuditService {

    void updateStatusMessageAboutToBeSent(PushNotificationMessage pushNotificationMessage);

    void updateStatusMessageSentSuccessfully(PushNotificationMessage pushNotificationMessage);

    void updateStatusMessageSentFailure(PushNotificationMessage pushNotificationMessage);

    void updateStatusMessageFailureRetry(PushNotificationMessage pushNotificationMessage);

    void updateAuditStatus(CampaignNotificationAuditMessage auditMessage);
}
