package com.yazino.engagement.campaign.application;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EmailCampaignDeliverMessage;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.email.infrastructure.CampaignCommanderClient;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

public class EmailCampaignUploaderAdapter implements ChannelCampaignDeliveryAdapter {


    private static final Logger LOG = LoggerFactory.getLogger(EmailCampaignUploaderAdapter.class);
    private final CampaignNotificationDao campaignNotificationDao;
    private final CampaignCommanderClient campaignCommanderClient;
    private final CampaignContentService campaignContentService;
    private final QueuePublishingService<EmailCampaignDeliverMessage> emailCampaignDeliverMessageQueuePublishingService;
    private final YazinoConfiguration configuration;

    @Autowired
    public EmailCampaignUploaderAdapter(final CampaignNotificationDao campaignNotificationDao,
                                        final CampaignCommanderClient campaignCommanderClient,
                                        final CampaignContentService campaignContentService,
                                        @Qualifier("emailCampaignDeliverMessageQueuePublishingService")
                                        QueuePublishingService<EmailCampaignDeliverMessage> emailCampaignDeliverMessageQueuePublishingService,
                                        final YazinoConfiguration configuration) {

        this.campaignNotificationDao = campaignNotificationDao;
        this.campaignCommanderClient = campaignCommanderClient;
        this.campaignContentService = campaignContentService;
        this.emailCampaignDeliverMessageQueuePublishingService = emailCampaignDeliverMessageQueuePublishingService;
        this.configuration = configuration;
    }

    @Override
    public ChannelType getChannel() {
        return ChannelType.EMAIL;
    }

    @Override
    //This doesn't actually deliver the messages. it uploads them to EV and puts a message on another queue to deliver the messages
    public void sendMessageToPlayers(final CampaignDeliverMessage campaignDeliverMessage) {
        Boolean isEmailEnabled = configuration.getBoolean("emailvision.campaign.enabled", Boolean.FALSE);

        if (isEmailEnabled) {
            List<EmailTarget> playersList = campaignNotificationDao.getEligibleEmailTargets(campaignDeliverMessage.getCampaignRunId());
            LOG.debug("segment size {} for {}", playersList.size(), campaignDeliverMessage);

            if (playersList.size() > 0) {
                final Long uploadId = campaignCommanderClient.addEmailAddresses(playersList,
                        campaignDeliverMessage.getCampaignRunId(),
                        campaignContentService.getEmailListName(campaignDeliverMessage.getCampaignRunId()));

                if (configuration.getBoolean("emailvision.campaign.deliver")) {
                    LOG.info("sending emailvision campaign run {} upload {} is enabled so firing message to publishing service",
                            campaignDeliverMessage.getCampaignRunId(),
                            campaignDeliverMessage.getCampaignRunId(),
                            uploadId);
                    Map<NotificationChannelConfigType, String> channelConfig = campaignContentService.getChannelConfig(campaignDeliverMessage.getCampaignRunId());
                    emailCampaignDeliverMessageQueuePublishingService.send(
                            new EmailCampaignDeliverMessage(campaignDeliverMessage.getCampaignRunId(),
                                    uploadId,
                                    channelConfig.get(NotificationChannelConfigType.TEMPLATE),
                                    channelConfig.get(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED)));
                } else {
                    LOG.info("not sending emailvision campaign run {} upload {} as it is disabled",
                            campaignDeliverMessage.getCampaignRunId(),
                            uploadId);
                }
            } else {
                LOG.warn("No targets for campaign exiting {}", campaignDeliverMessage);
            }
        } else {
            LOG.warn("emailvision.campaign.enabled is false set to true if you want to send email campaigns");
        }
    }
}
