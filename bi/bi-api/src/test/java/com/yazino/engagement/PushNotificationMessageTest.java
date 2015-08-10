package com.yazino.engagement;

import com.yazino.util.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PushNotificationMessageTest {

    public static final String EMPTY_STRING = "";
    public static final String TARGET_TOKEN = "1A2B3C";
    public static final String BUNDLE = "com.yazino.YazinoApp";
    public static final Long CAMPAIGN_RUN_ID= 100L;

    private final JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void shouldSerialiseAndDeserialiseUsingJson() {
        Map<String, String> contentMap = newHashMap();
        contentMap.put("message", "Some Random Message");
        PlayerTarget playerTarget = new PlayerTarget("SLOTS", EMPTY_STRING, BigDecimal.TEN, TARGET_TOKEN, BUNDLE, null);
        PushNotificationMessage underTest = new PushNotificationMessage(playerTarget, contentMap, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, CAMPAIGN_RUN_ID);

        final String serialized = jsonHelper.serialize(underTest);
        final PushNotificationMessage deSerializedMessage = jsonHelper.deserialize(PushNotificationMessage.class, serialized);

        assertThat(deSerializedMessage, is(equalTo(underTest)));
    }


}
