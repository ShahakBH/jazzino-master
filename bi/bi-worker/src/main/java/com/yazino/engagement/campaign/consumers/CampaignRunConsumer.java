package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.campaign.application.CampaignService;
import com.yazino.engagement.campaign.domain.CampaignRunMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CampaignRunConsumer implements QueueMessageConsumer<CampaignRunMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignRunConsumer.class);
    private final CampaignService campaignService;

    @Autowired
    public CampaignRunConsumer(CampaignService service) {

        this.campaignService = service;
    }

    @Override
    public void handle(CampaignRunMessage campaignRunMessage) {
        try {
            LOG.debug("consuming campaignRunMessage");

            campaignService.runCampaign(campaignRunMessage.getCampaignId(),
                    defaultToNow(campaignRunMessage.getReportTime()));
        } catch (Exception e) {
            LOG.error("problem with consuming runCampaignMessage {}", campaignRunMessage, e);
        }
    }

    private DateTime defaultToNow(final Date reportTime) {
        if (reportTime != null) {
            return new DateTime(reportTime);
        }
        return new DateTime();
    }
}
