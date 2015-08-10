package com.yazino.engagement;

import com.yazino.util.JsonHelper;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CampaignDeliverMessageTest {
    @Test
        public void ShouldSerialiseAndDeserialise(){
            final CampaignDeliverMessage underTest = new CampaignDeliverMessage(1l, ChannelType.IOS);
            final JsonHelper jsonHelper = new JsonHelper();
            final String serialized = jsonHelper.serialize(underTest);
            final CampaignDeliverMessage deserialized = jsonHelper.deserialize(CampaignDeliverMessage.class, serialized);
            assertThat(deserialized.getMessageType(),equalTo(underTest.getMessageType()));
            assertThat(deserialized.getCampaignRunId(), equalTo(underTest.getCampaignRunId()));
        }
}
