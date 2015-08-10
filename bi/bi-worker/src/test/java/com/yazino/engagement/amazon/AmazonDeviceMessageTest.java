package com.yazino.engagement.amazon;

import com.yazino.yaps.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AmazonDeviceMessageTest {
    private JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void amazonDeviceMessageShouldSerializeAndDeSerialize() {
        AmazonDeviceMessage message = new AmazonDeviceMessage(BigDecimal.TEN, "registration id", "title", "ticker", "message", "slots", 12l);

        String serializedString = jsonHelper.serialize(message);

        AmazonDeviceMessage deSerializedMessage = jsonHelper.deserialize(AmazonDeviceMessage.class, serializedString);

        assertThat(deSerializedMessage, is(message));
    }
}
