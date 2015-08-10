package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.PushNotificationMessage;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.yazino.engagement.campaign.consumers.MessageValidator.isValid;
import static com.yazino.engagement.campaign.domain.MessageContentType.TIME_TO_LIVE_IN_SECS;

public class NotificationCampaignConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationCampaignConsumer.class);

    protected int calculateTimeToLive(PushNotificationMessage message) {

        Map<String, String> contentMap = message.getContent();

        String timeToLiveAsString = contentMap.get(TIME_TO_LIVE_IN_SECS.getKey());

        int timeToLive;
        try {
            timeToLive = Integer.parseInt(timeToLiveAsString);
            if (timeToLive <= 0) {
                LOG.error("Invalid time to live, hence replacing with default");
                timeToLive = DateTimeConstants.SECONDS_PER_DAY;
            }
        } catch (NumberFormatException e) {
            timeToLive = DateTimeConstants.SECONDS_PER_DAY;
        }
        return timeToLive;
    }

    protected boolean isValidFormat(PushNotificationMessage message) {
        MessageValidatorStatus messageValidatorStatus = isValid(message);
        if (messageValidatorStatus != null && messageValidatorStatus.getStatus()) {
            return true;
        } else {
            if (messageValidatorStatus != null) {
                LOG.error("Validation failed with {}", messageValidatorStatus.getDescription());
            } else {
                LOG.error("Validation failed as returned MessageValidatorStatus is null");
            }
            return false;
        }
    }
}
