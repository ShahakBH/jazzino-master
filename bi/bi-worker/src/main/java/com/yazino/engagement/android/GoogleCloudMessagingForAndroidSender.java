package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.yazino.android.GoogleCloudMessagingConstants;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.facebook.FacebookAppRequestEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;

@Component("googleCloudMessagingForAndroidSender")
public class GoogleCloudMessagingForAndroidSender {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudMessagingForAndroidSender.class);

    protected static final String TICKER_MESSAGE_KEY = "ticker";
    protected static final String APP_REQUEST_KEY = "appRequestId";
    protected static final String DEFAULT_MESSAGE_TYPE = "m";
    protected static final int DEFAULT_RETRY_COUNT = 5;

    private Sender sender;
    private final String apiKey;

    @Autowired
    public GoogleCloudMessagingForAndroidSender(Sender sender, @Value("${google-cloud-messaging.api-key}") String apiKey) {
        this.sender = sender;
        this.apiKey = apiKey;
    }

    public GoogleCloudMessagingResponse sendRequest(FacebookAppRequestEnvelope requestEnvelope,
                                                    String registrationId,
                                                    int appRequestId,
                                                    int secondsToLive) throws IOException {
        checkArgument(secondsToLiveIsValid(secondsToLive));

        Message message = new Message.Builder()
                .addData(TITLE.getKey(), requestEnvelope.getTitle())
                .addData(MESSAGE.getKey(), requestEnvelope.getMessage())
                .addData(TYPE.getKey(), DEFAULT_MESSAGE_TYPE)
                .addData(TICKER_MESSAGE_KEY, requestEnvelope.getDescription())
                .addData(APP_REQUEST_KEY, Integer.toString(appRequestId))
                .collapseKey(Integer.toString(appRequestId))
                .timeToLive(secondsToLive)
                .build();
        LOG.debug("Sending message {} to registration id {} using apiKey {}", message, registrationId, apiKey);
        Result result = sender.send(message, registrationId, DEFAULT_RETRY_COUNT);
        return encapsulateResponse(result);
    }

    private boolean secondsToLiveIsValid(int secondsToLive) {
        return secondsToLive >= 0 && secondsToLive <= GoogleCloudMessagingConstants.MAX_SECONDS_TO_LIVE;
    }

    public GoogleCloudMessagingResponse sendRequest(PushNotificationMessage pushNotificationMessage, int secondsToLive) throws IOException {
        checkArgument(secondsToLiveIsValid(secondsToLive));

        Map<String, String> messageContent = pushNotificationMessage.getContent();
        PlayerTarget playerTarget = pushNotificationMessage.getPlayerTarget();

        Message message = new Message.Builder()
                .addData(TITLE.getKey(), messageContent.get(TITLE.getKey()))
                .addData(MESSAGE.getKey(), messageContent.get(MESSAGE.getKey()))
                .addData(TYPE.getKey(), DEFAULT_MESSAGE_TYPE)
                .addData(TICKER_MESSAGE_KEY, messageContent.get(DESCRIPTION.getKey()))
                .addData(APP_REQUEST_KEY, pushNotificationMessage.getCampaignRunId().toString())
                .collapseKey(pushNotificationMessage.getCampaignRunId().toString())
                .timeToLive(secondsToLive)
                .build();

        LOG.debug("Sending message {} with secondsToLive {}", message, secondsToLive);
        Result result = sender.send(message, playerTarget.getTargetToken(), DEFAULT_RETRY_COUNT);
        return encapsulateResponse(result);
    }

    private GoogleCloudMessagingResponse encapsulateResponse(Result result) {
        GoogleCloudMessagingResponse response = new GoogleCloudMessagingResponse();
        response.setCanonicalRegistrationId(result.getCanonicalRegistrationId());
        response.setErrorCode(result.getErrorCodeName());
        response.setMessageId(result.getMessageId());
        return response;
    }
}
