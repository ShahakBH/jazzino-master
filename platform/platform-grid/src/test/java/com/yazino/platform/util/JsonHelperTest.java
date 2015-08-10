package com.yazino.platform.util;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class JsonHelperTest {
    @Test
    public void shouldSerializeLists() {
        assertEquals("[\"a\",\"b\",\"c\"]", new JsonHelper().serialize(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void shouldSerializeDateTimes() {
        final String expectedTime = String.valueOf(new Date(0).getTime());
        final DateTime time = new DateTime(0);
        assertEquals(expectedTime, new JsonHelper().serialize(time));
    }
}
