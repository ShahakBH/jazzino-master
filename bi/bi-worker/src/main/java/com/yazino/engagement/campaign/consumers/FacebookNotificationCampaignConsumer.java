package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.dao.FacebookExclusionsDao;
import com.yazino.engagement.campaign.reporting.application.CampaignNotificationAuditService;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import com.yazino.engagement.facebook.FacebookAppToUserRequestStatus;
import com.yazino.engagement.facebook.FacebookRequestSender;
import com.yazino.engagement.facebook.FacebookResponse;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;
import strata.server.lobby.api.facebook.FacebookDataContainerBuilder;

import java.util.Map;

import static com.yazino.engagement.campaign.domain.MessageContentType.*;

@Service("facebookNotificationCampaignConsumer")
public class FacebookNotificationCampaignConsumer extends NotificationCampaignConsumer implements QueueMessageConsumer<PushNotificationMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookNotificationCampaignConsumer.class);

    private final FacebookRequestSender facebookRequestSender;
    private final CampaignNotificationAuditService campaignNotificationAuditService;
    private CampaignNotificationDao campaignNotificationDao;
    private FacebookExclusionsDao facebookExclusionsDao;

    @Autowired
    public FacebookNotificationCampaignConsumer(FacebookRequestSender facebookRequestSender,
                                                CampaignNotificationAuditService campaignNotificationAuditService,
                                                final CampaignNotificationDao campaignNotificationDao,
                                                final FacebookExclusionsDao facebookExclusionsDao) {
        this.facebookRequestSender = facebookRequestSender;
        this.campaignNotificationAuditService = campaignNotificationAuditService;
        this.campaignNotificationDao = campaignNotificationDao;
        this.facebookExclusionsDao = facebookExclusionsDao;
    }

    @Override
    public void handle(PushNotificationMessage message) {
        LOG.debug("Facebook notification consumer is attempting to consume a {}", message);
        try {
            if (isValidFormat(message)) {
                PlayerTarget playerTarget = message.getPlayerTarget();
                Map<String, String> contentMap = message.getContent();

                campaignNotificationAuditService.updateStatusMessageAboutToBeSent(message);

                FacebookAppRequestEnvelope facebookEnvelope = new FacebookAppRequestEnvelope(
                        contentMap.get(TITLE.getKey()),
                        contentMap.get(DESCRIPTION.getKey()),
                        playerTarget.getExternalId(),
                        playerTarget.getGameType(),
                        contentMap.get(MESSAGE.getKey()),
                        new FacebookDataContainerBuilder()
                                .withType(FacebookAppToUserRequestType.Engagement)
                                .withTrackingRef(TRACKING.getKey()).build().toJsonString(),
                        new DateTime().plusSeconds(calculateTimeToLive(message)));

                FacebookMessageType messageType = getMessageType(message.getChannel());
                FacebookResponse facebookResponse = facebookRequestSender.sendRequest(messageType, facebookEnvelope);
                if (facebookResponse != null && (facebookResponse.getStatus() == FacebookAppToUserRequestStatus.SENT)) {
                    campaignNotificationAuditService.updateStatusMessageSentSuccessfully(message);
                } else {
                    //log failure to the FacebookExceptions
                    LOG.debug("Message sending failed for message: {} with response {}", message, facebookResponse);
                    facebookExclusionsDao.logFailureInSendingFacebookNotification(playerTarget.getPlayerId(), playerTarget.getGameType());
                    campaignNotificationAuditService.updateStatusMessageSentFailure(message);
                }
            } else {
                LOG.error("Message is not valid: {}", message);
            }
        } catch (Exception e) {
            LOG.error(" Error while sending Facebook message {}", message, e);
            campaignNotificationAuditService.updateStatusMessageSentFailure(message);
        }
    }

    private FacebookMessageType getMessageType(ChannelType channelType) {
        switch (channelType) {
            case FACEBOOK_APP_TO_USER_NOTIFICATION:
                return FacebookMessageType.APP_TO_USER_NOTIFICATION;
            case FACEBOOK_APP_TO_USER_REQUEST:
                return FacebookMessageType.APP_TO_USER_REQUEST;
            default:
                throw new IllegalArgumentException("not a Facebook ChannelType: " + channelType);
        }
    }

}
