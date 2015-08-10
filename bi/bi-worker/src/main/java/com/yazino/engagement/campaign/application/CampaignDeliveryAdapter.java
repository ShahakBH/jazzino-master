package com.yazino.engagement.campaign.application;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.campaign.publishers.PushNotificationPublisher;
import com.yazino.engagement.mobile.MobileDeviceCampaignDao;
import com.yazino.platform.Platform;
import com.yazino.platform.table.GameTypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import strata.server.operations.repository.GameTypeRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.engagement.ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID;
import static com.yazino.engagement.ChannelType.IOS;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CampaignDeliveryAdapter implements ChannelCampaignDeliveryAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignDeliveryAdapter.class);

    private final CampaignNotificationDao campaignNotificationDao;
    private final MobileDeviceCampaignDao mobileDeviceCampaignDao;

    private final GameTypeRepository gameTypeRepository;

    private final PushNotificationPublisher pushNotificationPublisher;
    private final CampaignContentService campaignContentService;
    private final ChannelType channelType;

    public CampaignDeliveryAdapter(CampaignNotificationDao campaignNotificationDao,
                                   MobileDeviceCampaignDao mobileDeviceCampaignDao,
                                   GameTypeRepository gameTypeRepository,
                                   CampaignContentService campaignContentService,
                                   PushNotificationPublisher pushNotificationPublisher,
                                   ChannelType channelType) {

        this.campaignNotificationDao = campaignNotificationDao;
        this.mobileDeviceCampaignDao = mobileDeviceCampaignDao;
        this.gameTypeRepository = gameTypeRepository;
        this.pushNotificationPublisher = pushNotificationPublisher;
        this.campaignContentService = campaignContentService;
        this.channelType = channelType;
    }

    @Override
    public ChannelType getChannel() {
        return channelType;
    }

    @Override
    public void sendMessageToPlayers(CampaignDeliverMessage campaignDeliverMessage) {
        final List<PlayerTarget> playersList;
        //TODO break this adapter into separate adapters for Android, Facebook, iOS
        if (campaignDeliverMessage.getChannel() == GOOGLE_CLOUD_MESSAGING_FOR_ANDROID) {
            playersList = mobileDeviceCampaignDao.getEligiblePlayerTargets(
                    campaignDeliverMessage.getCampaignRunId(),
                    Platform.ANDROID,
                    campaignDeliverMessage.getFrom(),
                    campaignDeliverMessage.getTo());

        } else if (campaignDeliverMessage.getChannel() == IOS) {
            playersList = mobileDeviceCampaignDao.getEligiblePlayerTargets(
                    campaignDeliverMessage.getCampaignRunId(), Platform.IOS,
                    campaignDeliverMessage.getFrom(),
                    campaignDeliverMessage.getTo());

        } else {
            playersList = campaignNotificationDao.getEligiblePlayerTargets(
                    campaignDeliverMessage.getCampaignRunId(),
                    getChannel());
        }
        LOG.debug("segment size {} for {}", playersList.size(), campaignDeliverMessage);

        Map<String, GameTypeInformation> gameTypes = gameTypeRepository.getGameTypes();
        final Map<String, String> campaignContent = campaignContentService.getContent(
                campaignDeliverMessage.getCampaignRunId());

        final Map<NotificationChannelConfigType, String> channelConfig = campaignContentService.getChannelConfig(
                campaignDeliverMessage.getCampaignRunId());
        final Set<String> allowedGameTypes;
        if (!isBlank(channelConfig.get(NotificationChannelConfigType.GAME_TYPE_FILTER))) {
            allowedGameTypes = newHashSet(channelConfig.get(NotificationChannelConfigType.GAME_TYPE_FILTER).split(","));
        } else {
            allowedGameTypes = Collections.emptySet();
        }

        for (PlayerTarget playerTarget : playersList) {
            if (allowedGameTypes.size() > 0 && !allowedGameTypes.contains(playerTarget.getGameType())) {
                LOG.debug("Skipping message as not in game_type list gameType={}", playerTarget.getGameType());
                continue;
            }

            final Map<String, String> personalData = playerTarget.getCustomData();

            final Map<String, String> personalisedContent = campaignContentService.personaliseContentData(
                    campaignContent, personalData, gameTypes.get(playerTarget.getGameType()).getGameType());

            PushNotificationMessage message = new PushNotificationMessage(
                    playerTarget,
                    personalisedContent,
                    channelType,
                    campaignDeliverMessage.getCampaignRunId());

            LOG.debug("Adapter={}, message Channel ={}", this.getChannel(), message.getChannel());
            pushNotificationPublisher.sendPushNotification(message);
        }
    }
}
