package com.yazino.engagement.campaign.reporting.application;

import org.joda.time.DateTime;

public interface AuditDeliveryService {
    void auditCampaignRun(Long campaignId,
                          Long campaignRunId,
                          String name,
                          Integer size,
                          final Long promoId,
                          String status,
                          String message,
                          final DateTime reportTime);
}
