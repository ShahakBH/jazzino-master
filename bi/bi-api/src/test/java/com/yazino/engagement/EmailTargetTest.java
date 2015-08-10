package com.yazino.engagement;

import com.yazino.util.JsonHelper;
import org.junit.Test;

import java.util.HashMap;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EmailTargetTest {
    @Test
    public void objectShouldSerialise(){
        final HashMap<String,Object> content = newHashMap();
        content.put("yo", "momma");
        final EmailTarget emailTarget = new EmailTarget("word", "to", content);
        final String serialised = new JsonHelper().serialize(emailTarget);
        assertThat(serialised, is(equalTo("{\"emailAddress\":\"word\",\"displayName\":\"to\",\"content\":{\"yo\":\"momma\"}}")));
    }

    @Test
    public void serialisedObjectShouldDeserialise(){
        final EmailTarget deserialized = new JsonHelper().deserialize(EmailTarget.class,
                "{\"emailAddress\":\"word\",\"displayName\":\"to\",\"content\":{\"yo\":\"momma\"}}");
        final HashMap<String,Object > content = newHashMap();
        content.put("yo", "momma");
        final EmailTarget emailTarget = new EmailTarget("word", "to", content);
        assertThat(emailTarget, equalTo(deserialized));
    }
}
