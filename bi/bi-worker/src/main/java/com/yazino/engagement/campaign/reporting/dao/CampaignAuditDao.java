package com.yazino.engagement.campaign.reporting.dao;

import org.joda.time.DateTime;

public interface CampaignAuditDao {
    void persistCampaignRun(Long campaignId,
                            Long campaignRunId,
                            String name,
                            int size,
                            DateTime timestamp,
                            String status,
                            String message,
                            final Long promoId);
}
