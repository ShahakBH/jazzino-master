package com.yazino.engagement.campaign.application;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;

public interface ChannelCampaignDeliveryAdapter {
    ChannelType getChannel();

    void sendMessageToPlayers(CampaignDeliverMessage message);
}
