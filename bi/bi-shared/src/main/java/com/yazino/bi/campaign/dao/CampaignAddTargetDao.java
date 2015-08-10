package com.yazino.bi.campaign.dao;

import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.engagement.campaign.domain.PlayerWithContent;

import java.math.BigDecimal;
import java.util.Set;

public interface CampaignAddTargetDao {
    void savePlayersToCampaign(Long campaignId,
                               Set<BigDecimal> expectedPlayerIds);

    int fetchCampaignTargets(Long campaignId, BatchVisitor<PlayerWithContent> visitor);

    Integer numberOfTargetsInCampaign(Long campaignId);
}
