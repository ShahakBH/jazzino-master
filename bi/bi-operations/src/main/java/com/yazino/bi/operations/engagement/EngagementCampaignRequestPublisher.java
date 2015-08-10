package com.yazino.bi.operations.engagement;

import com.yazino.engagement.EngagementCampaignStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class EngagementCampaignRequestPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(EngagementCampaignRequestPublisher.class);
    private static final int START_AT_FIRST_ROW = 0;

    private final EngagementCampaignDao appRequestDao;

    public EngagementCampaignRequestPublisher(final EngagementCampaignDao engagementCampaignDao) {
        checkNotNull(engagementCampaignDao);
        this.appRequestDao = engagementCampaignDao;
    }

    protected boolean engagementCampaignCanBePublished(final EngagementCampaign engagementCampaign) {
        if (engagementCampaign.getStatus() == EngagementCampaignStatus.SENT) {
            LOG.debug("Attempted to publish EngagementCampaign that has already been sent. campaignId={}", engagementCampaign.getId());
            return false;
        }
        if (engagementCampaign.getStatus() == EngagementCampaignStatus.PROCESSING) {
            LOG.debug("Attempted to publish EngagementCampaign that is being processed. campaignId={}", engagementCampaign.getId());
            return false;
        }
        return true;
    }

    protected List<AppRequestTarget> findTargetsFor(EngagementCampaign engagementCampaign) {
        return appRequestDao.findAppRequestTargetsById(engagementCampaign.getId(), START_AT_FIRST_ROW, Integer.MAX_VALUE);
    }

    protected void setStateToSent(final EngagementCampaign engagementCampaign) {
        engagementCampaign.setStatus(EngagementCampaignStatus.SENT);
        engagementCampaign.setSent(new DateTime());
        appRequestDao.update(engagementCampaign);
    }

    protected void setStateToProcessing(final EngagementCampaign engagementCampaign) {
        engagementCampaign.setStatus(EngagementCampaignStatus.PROCESSING);
        appRequestDao.update(engagementCampaign);
    }
}
