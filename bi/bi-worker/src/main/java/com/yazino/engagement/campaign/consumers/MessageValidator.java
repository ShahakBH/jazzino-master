package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

import static com.yazino.engagement.campaign.domain.MessageContentType.*;

public final class MessageValidator {

    private MessageValidator() {
        throw new UnsupportedOperationException("Constructor should NOT be used for utility classes");
    }

    public static MessageValidatorStatus isValid(final PushNotificationMessage message) {
        if (message == null) {
            return failureStatus("Message is null");
        } else if (message.getChannel() == null) {
            return failureStatus("Channel is null");
        } else if (message.getPlayerTarget() == null || message.getContent() == null) {
            return failureStatus("Player Target or Content is null");
        } else {
            switch (message.getChannel()) {
                case FACEBOOK_APP_TO_USER_REQUEST:
                case FACEBOOK_APP_TO_USER_NOTIFICATION:
                    return isValidForFacebook(message);
                case IOS:
                    return isValidForIos(message);
                case GOOGLE_CLOUD_MESSAGING_FOR_ANDROID:
                    return isValidForAndroid(message);
                default:
                    return failureStatus("Invalid Channel specified-" + message.getChannel());
            }
        }
    }

    private static MessageValidatorStatus isValidForFacebook(final PushNotificationMessage message) {
        PlayerTarget playerTarget = message.getPlayerTarget();
        Map<String, String> content = message.getContent();

        if (StringUtils.isEmpty(playerTarget.getExternalId())) {
            return failureStatus("External Id is not valid:" + playerTarget.getExternalId());
        } else if (StringUtils.isEmpty(playerTarget.getGameType())) {
            return failureStatus("GameType is not valid:" + playerTarget.getGameType());
        } else if (StringUtils.isEmpty(content.get(MESSAGE.getKey()))) {
            return failureStatus("Message is not valid:" + content.get(MESSAGE.getKey()));
        } else if (StringUtils.isEmpty(content.get(TRACKING.getKey()))) {
            return failureStatus("Data is not valid:" + content.get(TRACKING.getKey()));
        }
        return successStatus();
    }

    private static MessageValidatorStatus isValidForAndroid(final PushNotificationMessage message) {

        PlayerTarget playerTarget = message.getPlayerTarget();
        Map<String, String> content = message.getContent();

        if (StringUtils.isEmpty(playerTarget.getTargetToken())) {
            return failureStatus("Registration Id is not valid:" + content.get(playerTarget.getTargetToken()));
        } else if (StringUtils.isEmpty(content.get(TITLE.getKey()))) {
            return failureStatus("Title is not valid:" + content.get(TITLE.getKey()));
        } else if (StringUtils.isEmpty(content.get(MESSAGE.getKey()))) {
            return failureStatus("Message is not valid:" + content.get(MESSAGE.getKey()));
        } else if (StringUtils.isEmpty(content.get(DESCRIPTION.getKey()))) {
            return failureStatus("Description is not valid:" + content.get(DESCRIPTION.getKey()));
        }
        return successStatus();
    }

    private static MessageValidatorStatus isValidForIos(final PushNotificationMessage message) {

        PlayerTarget playerTarget = message.getPlayerTarget();
        Map<String, String> content = message.getContent();

        if (playerTarget.getPlayerId() == null) {
            return failureStatus("PlayerId is not valid :" + playerTarget.getPlayerId());
        } else if (StringUtils.isEmpty(playerTarget.getGameType())) {
            return failureStatus("GameType is not valid :" + playerTarget.getGameType());
        } else if (StringUtils.isEmpty(playerTarget.getTargetToken())) {
            return failureStatus("Device Token is not valid :" + playerTarget.getTargetToken());
        } else if (StringUtils.isEmpty(playerTarget.getBundle())) {
            return failureStatus("Bundle is not valid :" + playerTarget.getBundle());
        } else if (StringUtils.isEmpty(content.get(MESSAGE.getKey()))) {
            return failureStatus("Message is not valid :" + content.get(MESSAGE.getKey()));
        }

        return successStatus();
    }

    private static MessageValidatorStatus failureStatus(String description) {
        return new MessageValidatorStatus(false, description);
    }

    private static MessageValidatorStatus successStatus() {
        return new MessageValidatorStatus(true, "success");
    }
}
