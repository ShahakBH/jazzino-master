package com.yazino.engagement.campaign.reporting.dao;

import com.yazino.engagement.campaign.reporting.domain.CampaignNotificationAuditMessage;

import java.util.Set;

public interface CampaignNotificationAuditDao {
    void persist(Set<CampaignNotificationAuditMessage> campaignNotificationAuditMessages);
}
