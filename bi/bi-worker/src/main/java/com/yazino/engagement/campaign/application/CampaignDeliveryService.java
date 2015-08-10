package com.yazino.engagement.campaign.application;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CampaignDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignDeliveryService.class);

    private final QueuePublishingService<CampaignDeliverMessage> publishingService;

    @Autowired
    public CampaignDeliveryService(@Qualifier("campaignDeliverMessageQueuePublishingService")
                                   final QueuePublishingService<CampaignDeliverMessage> publishingService) {
        this.publishingService = publishingService;
    }

    public void deliverCommunications(CampaignDeliverMessage campaignDeliverMessage) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delivering communication: {}", campaignDeliverMessage);
        }
        publishingService.send(campaignDeliverMessage);
    }
}
