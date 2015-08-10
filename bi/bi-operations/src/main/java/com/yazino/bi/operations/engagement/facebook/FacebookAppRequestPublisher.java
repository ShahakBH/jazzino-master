package com.yazino.bi.operations.engagement.facebook;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.FacebookAppToUserMessage;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.bi.operations.engagement.AppRequestTarget;
import com.yazino.bi.operations.engagement.EngagementCampaign;
import com.yazino.bi.operations.engagement.EngagementCampaignDao;
import com.yazino.bi.operations.engagement.EngagementCampaignRequestPublisher;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class FacebookAppRequestPublisher extends EngagementCampaignRequestPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookAppRequestPublisher.class);

    private final QueuePublishingService<FacebookAppToUserMessage> queuePublishingService;

    @Autowired(required = true)
    public FacebookAppRequestPublisher(
            @Qualifier("fbAppToUserRequestQueuePublishingService")
            final QueuePublishingService<FacebookAppToUserMessage> queuePublishingService,
            final EngagementCampaignDao dao) {
        super(dao);
        notNull(queuePublishingService, "queuePublishingService is null");
        notNull(dao, "EngagementCampaignDao is Null");

        this.queuePublishingService = queuePublishingService;
    }

    public void sendRequest(final EngagementCampaign engagementCampaign) {
        checkNotNull(engagementCampaign, "engagementCampaign cannot be null");
        checkArgument(engagementCampaign.getChannelType() == ChannelType.FACEBOOK_APP_TO_USER_REQUEST
                || engagementCampaign.getChannelType() == ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION);

        LOG.debug("Sending facebook request for: {}", engagementCampaign);

        ChannelType channelType = engagementCampaign.getChannelType();
        if (engagementCampaignCanBePublished(engagementCampaign)) {
            setStateToProcessing(engagementCampaign);
            final List<AppRequestTarget> targets = findTargetsFor(engagementCampaign);

            for (AppRequestTarget target : targets) {
                final FacebookAppToUserMessage message = new FacebookAppToUserMessage(
                        messageTypeForChannel(channelType), engagementCampaign.getId(), target.getId());

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Publishing facebook app to user message: {}", message);
                }
                queuePublishingService.send(message);
            }
            setStateToSent(engagementCampaign);
        }
    }

    private FacebookMessageType messageTypeForChannel(ChannelType channelType) {
        switch (channelType) {
            case FACEBOOK_APP_TO_USER_REQUEST:
                return FacebookMessageType.APP_TO_USER_REQUEST;
            case FACEBOOK_APP_TO_USER_NOTIFICATION:
                return FacebookMessageType.APP_TO_USER_NOTIFICATION;
            default:
                throw new IllegalArgumentException(String.format("Unsupported channel type \"%s\"", channelType));
        }
    }

}
