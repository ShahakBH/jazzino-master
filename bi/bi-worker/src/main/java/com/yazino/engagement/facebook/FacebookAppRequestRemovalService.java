package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Publishes a delete request for each sent appRequests in expired engagement campaigns
 */
@Service
public class FacebookAppRequestRemovalService {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookAppRequestRemovalService.class);

    private final EngagementCampaignDao campaignDao;
    private final QueuePublishingService<FacebookDeleteAppRequestMessage> queuePublishingService;


    @Autowired
    public FacebookAppRequestRemovalService(
            @Qualifier("fbDeleteRequestQueuePublishingService")
            final QueuePublishingService<FacebookDeleteAppRequestMessage> queuePublishingService,
            final EngagementCampaignDao campaignDao) {
        notNull(campaignDao);
        notNull(queuePublishingService);

        this.queuePublishingService = queuePublishingService;
        this.campaignDao = campaignDao;
    }

    @Scheduled(fixedDelay = 300000)
    public void removeExpiredAppRequestsFromFacebook() {
        LOG.info("Removing Expired App Requests");

        final List<Integer> campaignsToExpire = campaignDao.fetchCampaignsToExpire();

        for (Integer campaignToExpire : campaignsToExpire) {
            sendDeleteRequestsForCampaign(campaignToExpire);
        }
    }


    private void sendDeleteRequestsForCampaign(final Integer campaignId) {
        // ensure that another process isn't expiring this campaign
        // updateCampaignStatusToExpiring must only change the state where the current state is 'sent', if
        // it can not do this it will return false
        if (campaignDao.updateCampaignStatusToExpiring(campaignId)) {
            final List<AppRequestExternalReference> targetsToBeExpired =
                    campaignDao.fetchAppRequestExternalReferences(campaignId);

            for (AppRequestExternalReference appRequestExternalReference : targetsToBeExpired) {
                deleteAppRequest(appRequestExternalReference);
            }

            updateCampaignStatusToExpired(campaignId);
        }
    }

    private void deleteAppRequest(final AppRequestExternalReference appRequestExternalReference) {
        queuePublishingService.send(new FacebookDeleteAppRequestMessage(appRequestExternalReference));
    }

    public void updateCampaignStatusToExpired(final Integer campaignId) {
        campaignDao.updateCampaignStatus(campaignId, EngagementCampaignStatus.EXPIRED);
    }
}
