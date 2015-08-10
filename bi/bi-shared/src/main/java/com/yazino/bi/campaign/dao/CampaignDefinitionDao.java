package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;

import java.util.List;
import java.util.Map;

public interface CampaignDefinitionDao {

    CampaignDefinition fetchCampaign(final Long campaignId);

    Long save(CampaignDefinition campaignDefinition);

    Map<String, String> getContent(Long campaignId);

    void update(CampaignDefinition campaignDefinition);

    void setEnabledStatus(Long campaignId, boolean enabled);

    Map<NotificationChannelConfigType, String> getChannelConfig(Long campaignId);

    List<ChannelType> getChannelTypes(Long campaignId);
}
