package com.yazino.platform.playerevent;

import com.yazino.platform.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class PlayerEventTest {

    private JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void shouldSerializeToAndFromJson() {
        PlayerEvent event = new PlayerEvent(BigDecimal.ONE, PlayerEventType.NEW_LEVEL, "par1", "par2");
        String eventAsJson = "{\"playerId\":1,\"eventType\":\"NEW_LEVEL\",\"parameters\":[\"par1\",\"par2\"]}";
        assertEquals(eventAsJson, jsonHelper.serialize(event));
        assertEquals(event, jsonHelper.deserialize(PlayerEvent.class, eventAsJson));
    }

}
