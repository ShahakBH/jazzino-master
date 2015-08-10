package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.campaign.application.CampaignDeliveryService;
import com.yazino.engagement.campaign.domain.DelayedCampaignDeliverMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.joda.time.DateTime.now;

@Component
public class DelayedCampaignDeliveryConsumer implements QueueMessageConsumer<DelayedCampaignDeliverMessage> {
    private CampaignDeliveryService campaignDeliveryService;

    @Autowired
    public DelayedCampaignDeliveryConsumer(CampaignDeliveryService campaignDeliveryService) {
        this.campaignDeliveryService = campaignDeliveryService;
    }

    @Override
    public void handle(final DelayedCampaignDeliverMessage message) {
        //delayed enough?
        if (now().isAfter(message.getRunAfter())) {
            //put it on the proper queue
            campaignDeliveryService.deliverCommunications(message.getMessage());
        } // else it needs to go back on the queue
    }

}
