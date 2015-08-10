package com.yazino.engagement.campaign.reporting.application;

import com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignRunAuditMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AuditDeliveryServiceImpl implements AuditDeliveryService {
    private static final Logger LOG = LoggerFactory.getLogger(AuditDeliveryServiceImpl.class);

    private final QueuePublishingService<CampaignAuditMessage> campaignAuditDeliveryQueue;

    @Autowired
    public AuditDeliveryServiceImpl(
            @Qualifier("auditCampaignMessageQueuePublishingService") final QueuePublishingService<CampaignAuditMessage> campaignAuditDeliveryQueue) {
        Validate.notNull(campaignAuditDeliveryQueue);
        this.campaignAuditDeliveryQueue = campaignAuditDeliveryQueue;
    }

    @Override
    public void auditCampaignRun(final Long campaignId,
                                 final Long campaignRunId,
                                 final String name,
                                 final Integer size,
                                 final Long promoId,
                                 final String status,
                                 final String message, final DateTime reportTime) {
        LOG.debug("adding CampaignRunAuditMessage to the Audit Campaign Queue");
        final CampaignRunAuditMessage campaignRunAuditMessage = new CampaignRunAuditMessage(campaignId, campaignRunId, name, size, reportTime, promoId, status, message);
        campaignAuditDeliveryQueue.send(campaignRunAuditMessage);
    }
}
