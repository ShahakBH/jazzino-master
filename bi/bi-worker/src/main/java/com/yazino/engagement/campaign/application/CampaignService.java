package com.yazino.engagement.campaign.application;

import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.engagement.campaign.reporting.application.AuditDeliveryService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class CampaignService {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignService.class);

    private CampaignDefinitionDao campaignDefinitionDao;
    private CampaignRunDao campaignRunDao;
    private SegmentSelectorDao segmentSelectorDao;
    private CampaignDeliveryService campaignDeliveryService;
    private AuditDeliveryService auditDeliveryService;
    private PromotionCreationService promotionCreationService;
    private final CampaignContentService campaignContentService;
    private final CampaignAddTargetDao campaignAddTargetDao;

    @Autowired
    public CampaignService(final CampaignDefinitionDao campaignDefinitionDao,
                           final CampaignRunDao campaignRunDao,
                           final SegmentSelectorDao segmentSelectorDao,
                           final CampaignDeliveryService campaignDeliveryService,
                           final AuditDeliveryService auditDeliveryService,
                           final PromotionCreationService promotionCreationService,
                           final CampaignContentService campaignContentService,
                           final CampaignAddTargetDao campaignAddTargetDao) {

        this.campaignDefinitionDao = campaignDefinitionDao;
        this.campaignRunDao = campaignRunDao;
        this.segmentSelectorDao = segmentSelectorDao;
        this.campaignDeliveryService = campaignDeliveryService;
        this.auditDeliveryService = auditDeliveryService;
        this.promotionCreationService = promotionCreationService;
        this.campaignContentService = campaignContentService;
        this.campaignAddTargetDao = campaignAddTargetDao;
    }

    public Long runCampaign(final Long campaignId, final DateTime reportTime) {

        notNull(campaignId, "campaignId Should not be null");
        notNull(reportTime, "reportTime Should not be null");

        //so at this point, do we check that we're not running the hourly app_to_user stuffs??
        final CampaignDefinition campaign = campaignDefinitionDao.fetchCampaign(campaignId);
        final Long campaignRunId = campaignRunDao.createCampaignRun(campaignId, reportTime);
        campaignRunDao.purgeSegmentSelection(campaignRunId);
        int recipientCount = 0;

        LOG.info("running SQL: {} for campaign {} for time {}. ",
                campaign.getSegmentSelectionQuery(), campaign.getId(), reportTime);

        final Long promoId;
        if (campaign.hasPromo()) {
            promoId = promotionCreationService.createPromotionForCampaign(campaignId, Collections.<BigDecimal>emptyList());
            LOG.info("Promotion Created for campaignId = {}, campaign run id = {} with promoid {}", campaignId, campaignRunId, promoId);
        } else {
            promoId = null;
        }

        try {
            auditCampaignRun(campaign, campaignRunId, recipientCount, "started", null, "scheduled time: " + reportTime.toString());

            final BatchVisitor<PlayerWithContent> visitor = new CampaignRunBatchVisitor(campaignId, campaignRunId, promoId, campaign);
            recipientCount += segmentSelectorDao.fetchSegment(campaign.getSegmentSelectionQuery(), reportTime, visitor);
            recipientCount += campaignAddTargetDao.fetchCampaignTargets(campaignId, visitor);

        } catch (Exception e) {
            LOG.error("segment selection failed for campaignId {} @ {}, campaign run id {}", campaignId, new DateTime(), campaignRunId, e);
            auditCampaignRun(campaign, campaignRunId, 0, "segment selection failed", null, e.getMessage());
            return null;
        }

        campaignContentService.updateCustomDataFields(campaignRunId, campaign.getContent());

        if (!campaign.delayNotifications()) {
            sendDeliverMessageToQueueForEachChannel(campaign, campaignRunId);
        }
        auditCampaignRun(campaign, campaignRunId, recipientCount, "success", promoId, "scheduled time: " + reportTime.toString());

        LOG.debug("finished audit of Campaign run campaignId = {}, campaign run id = {}", campaignId, campaignRunId);
        return campaignRunId;
    }

    private void sendDeliverMessageToQueueForEachChannel(final CampaignDefinition campaign, final Long campaignRunId) {
        for (ChannelType channel : campaign.getChannels()) {
            LOG.debug("adding message for campaign run {} onto the {} queue", campaignRunId, channel.name());
            campaignDeliveryService.deliverCommunications(new CampaignDeliverMessage(campaignRunId, channel));
        }
    }

    private void auditCampaignRun(final CampaignDefinition campaign,
                                  final Long campaignRunId,
                                  final int size,
                                  final String message,
                                  final Long promoId,
                                  final String exceptionMessage) {
        auditDeliveryService.auditCampaignRun(campaign.getId(),
                campaignRunId,
                campaign.getName(),
                size,
                promoId,
                message,
                exceptionMessage, new DateTime());
    }

    private class CampaignRunBatchVisitor implements BatchVisitor<PlayerWithContent> {
        private final Long campaignId;
        private final Long campaignRunId;
        private final Long promoId;
        private final CampaignDefinition campaign;

        public CampaignRunBatchVisitor(final Long campaignId,
                                       final Long campaignRunId,
                                       final Long promoId,
                                       final CampaignDefinition campaign) {
            this.campaignId = campaignId;
            this.campaignRunId = campaignRunId;
            this.promoId = promoId;
            this.campaign = campaign;
        }

        @Override
        public void processBatch(final List<PlayerWithContent> batch) {
            try {
                campaignRunDao.addPlayers(campaignRunId, batch, campaign.delayNotifications());

                if (promoId != null) {
                    addBatchToPromotion(batch);
                }

            } catch (Exception e) {
                auditCampaignRun(campaign, campaignRunId, batch.size(), "Adding players to campaign run failed", null, e.getMessage());
            }
        }

        private void addBatchToPromotion(final List<PlayerWithContent> batch) {
            final Set<BigDecimal> batchPlayerIds = new HashSet<>();
            for (PlayerWithContent playerWithContent : batch) {
                batchPlayerIds.add(playerWithContent.getPlayerId());
            }
            promotionCreationService.addPlayersToPromotionForCampaign(campaignId, promoId, batchPlayerIds);
        }
    }
}
