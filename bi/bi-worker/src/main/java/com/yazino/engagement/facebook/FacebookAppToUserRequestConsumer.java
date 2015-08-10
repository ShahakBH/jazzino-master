package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.FacebookAppToUserMessage;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.Validate.notNull;

@Component("fbRequestConsumer")
public class FacebookAppToUserRequestConsumer implements QueueMessageConsumer<FacebookAppToUserMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookAppToUserRequestConsumer.class);

    private final FacebookRequestSender facebookRequestSender;

    private final EngagementCampaignDao appRequestConsumerDao;

    @Autowired
    public FacebookAppToUserRequestConsumer(final FacebookRequestSender facebookRequestSender,
                                            final EngagementCampaignDao appRequestConsumerDao) {
        notNull(facebookRequestSender, "FacebookRequestSender can not be Null");
        notNull(appRequestConsumerDao, "EngagementCampaignDao can not be Null");
        this.facebookRequestSender = facebookRequestSender;
        this.appRequestConsumerDao = appRequestConsumerDao;
    }

    @Override
    public void handle(final FacebookAppToUserMessage message) {
        LOG.debug("consuming facebook app to user message" + message);
        final FacebookAppRequestEnvelope facebookAppRequestEnvelope =
                appRequestConsumerDao.fetchAppRequestEnvelopeByCampaignAndTargetId(
                message.getAppRequestId(), message.getTargetId());
        if (facebookAppRequestEnvelope == null) {
            LOG.debug("Could not load facebook app request message for targetId={}, appRequestId={}",
                    message.getTargetId(), message.getAppRequestId());
            return;
        }

        if (facebookAppRequestEnvelope.getGameType() == null || facebookAppRequestEnvelope.getGameType().trim().equalsIgnoreCase("NULL")) {
            LOG.warn("Invalid game type {} for message {}", facebookAppRequestEnvelope.getGameType(), facebookAppRequestEnvelope);
            return;
        }

        final FacebookResponse response = facebookRequestSender.sendRequest(message.getMessageType(), facebookAppRequestEnvelope);
        if (response == null)   {
            LOG.error("Failed to get response from Facebook for post of appRequest={}, appRequestId={}",
                                message.getTargetId(), message.getAppRequestId());
            return;
        }

        if (message.getMessageType() != FacebookMessageType.APP_TO_USER_NOTIFICATION) {
            if (response.getStatus() == FacebookAppToUserRequestStatus.SENT) {
                appRequestConsumerDao.saveExternalReference(
                        message.getTargetId(),
                        response.getRequestId());
            }
        }
        logMessageStatus(message, facebookAppRequestEnvelope.getExternalId(), response);
    }

    private void logMessageStatus(final FacebookAppToUserMessage message,
                                  final String externalId,
                                  final FacebookResponse facebookResponse) {
        if (facebookResponse.getStatus() == FacebookAppToUserRequestStatus.SENT) {
            LOG.debug("Sent facebook app to user message. "
                    + "FacebookId={}, targetId={}, messageType={}, appRequestId={}, facebookRequestId={}",
                    externalId, message.getTargetId(), message.getMessageType(), message.getAppRequestId(), facebookResponse.getRequestId());
        } else if (facebookResponse.getStatus() == FacebookAppToUserRequestStatus.FAILED) {
            LOG.warn("Failed to send facebook app to user message. "
                    + "FacebookId={}, targetId={}, messageType={}, appRequestId={}",
                    externalId, message.getTargetId(), message.getMessageType(), message.getAppRequestId());
        }
    }
}
