package com.yazino.platform.opengraph;

import com.yazino.platform.JsonHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenGraphActionTest {

    @Test
    public void survivesSerialisationRoundTrip() {
        OpenGraphAction action = new OpenGraphAction("action1", new OpenGraphObject("object1", "http://object1"));
        JsonHelper jsonHelper = new JsonHelper();
        OpenGraphAction actionCopy = jsonHelper.deserialize(OpenGraphAction.class, jsonHelper.serialize(action));
        assertEquals(action, actionCopy);
    }
}
