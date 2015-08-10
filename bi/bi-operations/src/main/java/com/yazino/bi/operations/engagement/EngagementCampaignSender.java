package com.yazino.bi.operations.engagement;

import com.yazino.engagement.EngagementCampaignStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.yazino.bi.operations.engagement.facebook.FacebookAppRequestPublisher;

import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Enqueues engagement campaigns, adds a request for each target in the campaign.<br>
 * Call {@code sendAppRequest} to send
 * a single request. Method {@code sendDueAppRequests},
 * scheduled to run every 5 mins, will send all due scheduled requests.
 * That is their status is {@code AppRequestStatus.CREATED}
 * and their schedule date is now past.
 */
@Service
public class EngagementCampaignSender {
    private static final Logger LOG = LoggerFactory.getLogger(EngagementCampaignSender.class);

    private EngagementCampaignDao dao;
    private FacebookAppRequestPublisher facebookAppRequestPublisher;

    @Autowired
    public EngagementCampaignSender(final EngagementCampaignDao dao,
                                    final FacebookAppRequestPublisher facebookAppRequestPublisher) {
        notNull(dao, "dao was null");
        notNull(facebookAppRequestPublisher, "facebookAppRequestPublisher was null");
        this.dao = dao;
        this.facebookAppRequestPublisher = facebookAppRequestPublisher;
    }

    @Scheduled(fixedDelay = 300000)
    public void sendDueAppRequests() {
        LOG.info("Sending due App requests");

        final List<EngagementCampaign> dueEngagementCampaigns = dao.findDueEngagementCampaigns(new DateTime());
        for (EngagementCampaign dueEngagementCampaign : dueEngagementCampaigns) {
            sendAppRequest(dueEngagementCampaign);
        }
    }

    /**
     * Sends given app request.
     *
     * @param engagementCampaign campaign to publish
     * @return true if request sent, false otherwise.
     */
    public boolean sendAppRequest(final EngagementCampaign engagementCampaign) {
        notNull(engagementCampaign, "engagementCampaign is null");
        LOG.info("Sending app request: {}", engagementCampaign);

        if (engagementCampaign.getStatus() != EngagementCampaignStatus.CREATED) {
            LOG.info("Cannot send app request when state is: {}", engagementCampaign.getStatus().name());
            return false;
        }

        boolean sent = false;
        switch (engagementCampaign.getChannelType()) {
            case FACEBOOK_APP_TO_USER_NOTIFICATION:
            case FACEBOOK_APP_TO_USER_REQUEST:
                facebookAppRequestPublisher.sendRequest(engagementCampaign);
                sent = true;
                break;
            default:
                LOG.warn("Unsupported channel type: {}", engagementCampaign.getChannelType().name());
        }
        return sent;
    }

}
