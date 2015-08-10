package com.yazino.engagement.amazon;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.campaign.application.CampaignContentService;
import com.yazino.engagement.campaign.application.ChannelCampaignDeliveryAdapter;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.mobile.MobileDeviceCampaignDao;
import com.yazino.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

@Service
public class AmazonCampaignDeliveryService implements ChannelCampaignDeliveryAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonCampaignDeliveryService.class);

    private final MobileDeviceCampaignDao mobileDeviceCampaignDao;
    private final AmazonDeviceMessagingPublisher pushNotificationPublisher;
    private final CampaignContentService campaignContentService;


    @Autowired
    public AmazonCampaignDeliveryService(MobileDeviceCampaignDao mobileDeviceCampaignDao,
                                         CampaignContentService campaignContentService,
                                         AmazonDeviceMessagingPublisher pushNotificationPublisher) {

        this.mobileDeviceCampaignDao = mobileDeviceCampaignDao;
        this.pushNotificationPublisher = pushNotificationPublisher;
        this.campaignContentService = campaignContentService;
    }

    @Override
    public ChannelType getChannel() {
        return ChannelType.AMAZON_DEVICE_MESSAGING;
    }

    @Override
    public void sendMessageToPlayers(CampaignDeliverMessage campaignDeliverMessage) {
        LOG.debug("sending ADM Campaign");
        List<PlayerTarget> playersList = getEligiblePlayerTargets(campaignDeliverMessage);

        final Map<String, String> campaignContent = getCampaignContent(campaignDeliverMessage);
        final Map<NotificationChannelConfigType, String> campaignChannelConfig = getChannelCampaignConfig(
                campaignDeliverMessage);

        for (PlayerTarget playerTarget : playersList) {
            if (isAllowedGameType(getGameTypesForCampaign(campaignChannelConfig), playerTarget)) {
                Map<String, String> personalisedContent = campaignContentService.getPersonalisedContent(
                        campaignContent, playerTarget);
                sendMessageToTarget(campaignDeliverMessage, playerTarget, personalisedContent);
            } else {
                LOG.debug("Skipping message as not in game_type list gameType={}", playerTarget.getGameType());
            }
        }
    }

    private Set<String> getGameTypesForCampaign(
            final Map<NotificationChannelConfigType, String> campaignChannelConfig) {
        final Set<String> allowedGameTypes;
        if (campaignChannelConfig.get(NotificationChannelConfigType.GAME_TYPE_FILTER) != null) {
            allowedGameTypes = newHashSet(
                    campaignChannelConfig.get(NotificationChannelConfigType.GAME_TYPE_FILTER).split(","));
        } else {
            allowedGameTypes = Collections.emptySet();
        }

        return allowedGameTypes;
    }

    private void sendMessageToTarget(final CampaignDeliverMessage campaignDeliverMessage,
                                     final PlayerTarget playerTarget,
                                     final Map<String, String> personalisedContent) {

        AmazonDeviceMessage message = new AmazonDeviceMessage(
                playerTarget.getPlayerId(),
                playerTarget.getTargetToken(),
                personalisedContent.get(MessageContentType.TITLE.getKey()),
                personalisedContent.get(MessageContentType.TITLE.getKey()),
                personalisedContent.get(MessageContentType.MESSAGE.getKey()),
                "notify",
                campaignDeliverMessage.getCampaignRunId());

        pushNotificationPublisher.sendPushNotification(message);
    }

    private boolean isAllowedGameType(final Set<String> allowedGameTypes, final PlayerTarget playerTarget) {
        return allowedGameTypes.isEmpty() || allowedGameTypes.contains(playerTarget.getGameType());
    }

    private Map<NotificationChannelConfigType, String> getChannelCampaignConfig(
            final CampaignDeliverMessage campaignDeliverMessage) {
        return campaignContentService.getChannelConfig(campaignDeliverMessage.getCampaignRunId());
    }

    private Map<String, String> getCampaignContent(final CampaignDeliverMessage campaignDeliverMessage) {
        return campaignContentService.getContent(campaignDeliverMessage.getCampaignRunId());
    }

    private List<PlayerTarget> getEligiblePlayerTargets(final CampaignDeliverMessage campaignDeliverMessage) {
        List<PlayerTarget> eligiblePlayerTargets = mobileDeviceCampaignDao.getEligiblePlayerTargets(
                campaignDeliverMessage.getCampaignRunId(),
                Platform.AMAZON,
                campaignDeliverMessage.getFrom(),
                campaignDeliverMessage.getTo());
        LOG.debug("segment size {} for {}", eligiblePlayerTargets.size(), campaignDeliverMessage);
        return eligiblePlayerTargets;
    }
}
