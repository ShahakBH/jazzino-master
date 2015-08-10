package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.application.ChannelCampaignDeliveryAdapter;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CampaignDeliveryConsumer implements QueueMessageConsumer<CampaignDeliverMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignDeliveryConsumer.class);

    private Map<ChannelType, ChannelCampaignDeliveryAdapter> adapters = new HashMap<ChannelType, ChannelCampaignDeliveryAdapter>();

    @Autowired
    public CampaignDeliveryConsumer(final List<ChannelCampaignDeliveryAdapter> adapters) {
        for (ChannelCampaignDeliveryAdapter adapter : adapters) {
            this.adapters.put(adapter.getChannel(), adapter);
        }
    }

    @Override
    public void handle(final CampaignDeliverMessage message) {
        try {
            if (adapters.containsKey(message.getChannel())) {
                final ChannelCampaignDeliveryAdapter adapter = adapters.get(message.getChannel());

                LOG.debug("attempting to send {} through adapter for channel{}", message, adapter.getChannel());
                adapter.sendMessageToPlayers(message);
            } else {
                LOG.warn("unimplemented ChannelCampaignDeliveryAdapter {}", message.getChannel());
            }

        } catch (Exception e) {
            LOG.error("problem with handling CampaignDeliverMessage {}", message, e);
        }
    }

}
