package com.yazino.engagement.campaign.application;

import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.Validate.notNull;
import static org.joda.time.DateTime.now;

@Service
public class DelayedCampaignRunner {


    private final SegmentSelectorDao segmentSelectorDao;
    private QueuePublishingService<CampaignDeliverMessage> campaignDeliverMessageQueuePublishingService;
    private static final Logger LOG = LoggerFactory.getLogger(DelayedCampaignRunner.class);
    private final CampaignRunDao campaignRunDao;
    private final CampaignDefinitionDao campaignDefinitionDao;

    @Autowired
    public DelayedCampaignRunner(final SegmentSelectorDao segmentSelectorDao,
                                 final QueuePublishingService<CampaignDeliverMessage> campaignDeliverMessageQueuePublishingService,
                                 final CampaignRunDao campaignRunDao,
                                 final CampaignDefinitionDao campaignDefinitionDao) {
        notNull(campaignRunDao);
        notNull(campaignDeliverMessageQueuePublishingService);
        notNull(segmentSelectorDao);

        this.campaignDefinitionDao = campaignDefinitionDao;
        this.campaignRunDao = campaignRunDao;
        this.campaignDeliverMessageQueuePublishingService = campaignDeliverMessageQueuePublishingService;
        this.segmentSelectorDao = segmentSelectorDao;
    }

    @Scheduled(cron = "${strata.delayed-campaign-runner.timing}")
    public void reRun() {
        LOG.debug("rerunning delayed campaigns");
        final DateTime now = now().withMillisOfSecond(0);

        final Map<Long, Long> latestCampaignRuns = campaignRunDao.getLatestDelayedCampaignRunsInLast24Hours();
        LOG.debug("got the campaigns run ids:{}", latestCampaignRuns);

        segmentSelectorDao.updateSegmentDelaysForCampaignRuns(latestCampaignRuns.keySet(), now);

        for (Long campaignRunId : latestCampaignRuns.keySet()) {

            DateTime from = campaignRunDao.getLastRuntimeForCampaignRunIdAndResetTo(campaignRunId, now);
            LOG.debug("setting campaign:{} run:{} to rerun from {}", latestCampaignRuns.get(campaignRunId), campaignRunId, from);
            final List<ChannelType> channelTypes = campaignDefinitionDao.getChannelTypes(
                    latestCampaignRuns.get(campaignRunId));

            for (ChannelType channelType : channelTypes) {
                campaignDeliverMessageQueuePublishingService.send(
                        new CampaignDeliverMessage(campaignRunId, channelType, from.getMillis(), now.getMillis()));
            }


        }

    }

}
