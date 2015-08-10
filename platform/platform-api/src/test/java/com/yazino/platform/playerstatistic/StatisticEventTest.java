package com.yazino.platform.playerstatistic;

import com.yazino.platform.JsonHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatisticEventTest {

    private JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void shouldSerializeParameters() {
        final StatisticEvent underTest = new StatisticEvent("event", 0, 0, "p1", "p2", "p3");
        final String json = jsonHelper.serialize(underTest);
        final StatisticEvent actual = jsonHelper.deserialize(StatisticEvent.class, json);
        assertEquals(underTest, actual);
    }
}
