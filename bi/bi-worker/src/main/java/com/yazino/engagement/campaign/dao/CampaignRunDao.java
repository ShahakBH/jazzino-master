package com.yazino.engagement.campaign.dao;

import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CampaignRunDao {
    Long createCampaignRun(Long campaignId, final DateTime dateTime);

    void purgeSegmentSelection(Long campaignRunId);

    void addPlayers(Long campaignRunId, Collection<PlayerWithContent> toDeliver, final boolean delayNotifications);

    List<PlayerWithContent> fetchPlayers(Long campaignRunId);

    CampaignRun getCampaignRun(Long campaignRunId);

    Map<Long, Long> getLatestDelayedCampaignRunsInLast24Hours();

    DateTime getLastRuntimeForCampaignRunIdAndResetTo(Long campaignRunId, DateTime now);
}
