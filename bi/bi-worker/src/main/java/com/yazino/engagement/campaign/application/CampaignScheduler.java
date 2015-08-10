package com.yazino.engagement.campaign.application;

import com.yazino.bi.aggregator.HostUtils;
import com.yazino.bi.aggregator.LockDao;
import com.yazino.bi.campaign.dao.CampaignScheduleDao;
import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignScheduler.class);

    public static final String LOCK_NAME = "campaign_runner";
    private CampaignScheduleDao campaignScheduleDao;
    private CampaignRunService campaignRunService;
    private LockDao lockDao;
    private final YazinoConfiguration yazinoConfiguration;
    private String clientId;


    @Autowired
    public CampaignScheduler(
            final CampaignScheduleDao campaignScheduleDao,
            final CampaignRunService campaignRunService,
            final LockDao lockDao,
            final YazinoConfiguration yazinoConfiguration) {
        this.campaignScheduleDao = campaignScheduleDao;
        this.campaignRunService = campaignRunService;
        this.lockDao = lockDao;
        this.yazinoConfiguration = yazinoConfiguration;
        clientId = HostUtils.getHostName();
    }

    @Scheduled(fixedDelay = 60000)
    public void runScheduledCampaign() {
        if (yazinoConfiguration.getBoolean("engagement.campaigns.schedule.disabled", Boolean.FALSE)) {
            LOG.warn("engagement.campaigns.schedule.disabled property is set not running scheduled campaigns");
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking for due campaigns");
        }
        if (!lockDao.lock(LOCK_NAME, clientId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not obtain lock. Returning...");
            }
            return;
        }
        try {
            DateTime currentTs = new DateTime();
            final List<CampaignSchedule> dueCampaigns = campaignScheduleDao.getDueCampaigns(currentTs);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found {} campaigns to run", dueCampaigns.size());
            }
            for (CampaignSchedule campaign : dueCampaigns) {
                DateTime nextRunTs = campaign.calculateNextRunTs();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Preparing to run campaign {}. Updating nextRunTs to {}", campaign, nextRunTs);
                }
                campaignRunService.runCampaign(campaign.getCampaignId(), campaign.getNextRunTs().toDate());
                campaignScheduleDao.updateNextRunTs(campaign.getCampaignId(), nextRunTs);
            }
        } catch (Exception e) {
            LOG.error("Error handling campaigns", e);
        } finally {
            lockDao.unlock(LOCK_NAME, clientId);
        }
    }

}
