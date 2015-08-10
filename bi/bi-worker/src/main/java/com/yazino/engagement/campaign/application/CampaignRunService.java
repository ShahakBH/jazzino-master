package com.yazino.engagement.campaign.application;

import com.yazino.engagement.campaign.domain.CampaignRunMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class CampaignRunService {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignRunService.class);

    private final QueuePublishingService<CampaignRunMessage> publishingService;

    @Autowired
    public CampaignRunService(@Qualifier("runCampaignMessageQueuePublishingService") QueuePublishingService<CampaignRunMessage> publishingService) {
        this.publishingService = publishingService;
    }

    public void runCampaign(Long campaignId, final Date reportTime) {
        LOG.debug("Sending CampaignRunMessage for campaignId={} @{},", campaignId, reportTime);
        publishingService.send(new CampaignRunMessage(campaignId, reportTime));
    }
}
