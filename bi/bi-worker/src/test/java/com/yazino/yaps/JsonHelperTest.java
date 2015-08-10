package com.yazino.yaps;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

public class JsonHelperTest {
    @Test
    public void shouldSerializeLists() {
        Assert.assertEquals("[\"a\",\"b\",\"c\"]", new JsonHelper().serialize(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void shouldSerializeDateTimes() {
        final String expectedTime = String.valueOf(new Date(0).getTime());
        final DateTime time = new DateTime(0);
        Assert.assertEquals(expectedTime, new JsonHelper().serialize(time));
    }
}
