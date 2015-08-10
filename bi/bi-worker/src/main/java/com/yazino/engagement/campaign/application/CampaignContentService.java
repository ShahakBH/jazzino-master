package com.yazino.engagement.campaign.application;

import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.game.api.GameType;

import java.util.Map;

public interface CampaignContentService {
    void updateCustomDataFields(Long campaignRunId, Map<String, String> campaignContent);

    Map<String, String> personaliseContentData(Map<String, String> campaignContent, Map<String, String> customData, GameType gameType);

    Map<String, String> getPersonalisedContent(Map<String, String> campaignContent, PlayerTarget playerTarget);

    Map<String, String> getContent(Long campaignRunId);

    String getEmailListName(final Long campaignRunId);

    Map<NotificationChannelConfigType, String> getChannelConfig(Long campaignRunId);
}
