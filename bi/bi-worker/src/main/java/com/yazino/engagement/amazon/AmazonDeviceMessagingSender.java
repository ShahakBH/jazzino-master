package com.yazino.engagement.amazon;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Service
public class AmazonDeviceMessagingSender {
    public static final String API_AMAZON_COM = "api.amazon.com";
    public static final String COM_AMAZON_DEVICE_MESSAGING_ADMMESSAGE_1_0 = "com.amazon.device.messaging.ADMMessage@1.0";
    public static final String COM_AMAZON_DEVICE_MESSAGING_ADMSEND_RESULT_1_0 = "com.amazon.device.messaging.ADMSendResult@1.0";
    private final RestOperations restOperations;

    @Autowired
    public AmazonDeviceMessagingSender(@Qualifier("amazonRestOperations") final RestOperations restOperations) {
        Validate.notNull(restOperations);
        this.restOperations = restOperations;
    }


    public String sendMessage(final String registrationId,
                              String accessToken,
                              final String title,
                              final String tickerMessage,
                              final String message,
                              final Long campaignRunId) throws HttpClientErrorException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(asList(MediaType.APPLICATION_JSON));
        headers.set("Host", API_AMAZON_COM);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("X-Amzn-Type-Version", COM_AMAZON_DEVICE_MESSAGING_ADMMESSAGE_1_0);
        headers.set("X-Amzn-Accept-Type", COM_AMAZON_DEVICE_MESSAGING_ADMSEND_RESULT_1_0);

        Map<String, Map<String, String>> map = new HashMap<>();
        HashMap<String, String> dataParamMap = new HashMap<>();
        dataParamMap.put("title", title);
        dataParamMap.put("ticker", tickerMessage);
        dataParamMap.put("message", message);
        dataParamMap.put("campaignRunId", campaignRunId.toString());
        dataParamMap.put("action", "notify");
        map.put("data", dataParamMap);

        HttpEntity httpEntity = new HttpEntity<>(map, headers);

        return restOperations.postForObject(format("https://api.amazon.com/messaging/registrations/%s/messages",
                registrationId), httpEntity, String.class);
    }
}
