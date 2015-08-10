package com.yazino.engagement.campaign.reporting.consumers;

import com.yazino.engagement.campaign.reporting.domain.CampaignRunAuditMessage;
import com.yazino.engagement.campaign.reporting.dao.CampaignAuditDao;
import com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessageType;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessageType.valueOf;

@Service
public class CampaignAuditConsumer implements QueueMessageConsumer<CampaignAuditMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignAuditConsumer.class);
    private final CampaignAuditDao campaignAuditDao;

    @Autowired
    public CampaignAuditConsumer(CampaignAuditDao campaignAuditDao) {
        Validate.notNull(campaignAuditDao);
        this.campaignAuditDao = campaignAuditDao;
    }

    @Override
    public void handle(final CampaignAuditMessage message) {
        LOG.debug("Campaign Audit Consumer is Handling message {}", message);
        try {
            final CampaignAuditMessageType campaignAuditMessageType = valueOf(message.getMessageType().toString());

            switch (campaignAuditMessageType) {
                case CAMPAIGN_RUN_MESSAGE:
                    LOG.debug("consuming campaign run message");
                    persistCampaignRunMessage((CampaignRunAuditMessage) message);
                    break;
                default:
                    LOG.info("unsupported message type {}", message.getMessageType().toString());
                    throw new UnsupportedOperationException();
            }
        } catch (Exception e) {
            LOG.error("problem with saving CampaignAuditMessage {}", message, e);
        }
    }

    private void persistCampaignRunMessage(final CampaignRunAuditMessage message) {
        campaignAuditDao.persistCampaignRun(
                message.getCampaignId(),
                message.getCampaignRunId(),
                message.getName(),
                message.getSize(),
                message.getTimestamp(),
                message.getStatus(),
                message.getMessage(),
                message.getPromoId());
    }
}
