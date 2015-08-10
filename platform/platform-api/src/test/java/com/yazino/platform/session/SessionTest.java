package com.yazino.platform.session;


import com.yazino.platform.JsonHelper;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;

public class SessionTest {
    @Test
    @Ignore
    public void sessionShouldSerialize() {
        JsonHelper helper = new JsonHelper(false);
        final Session session = new Session(BigDecimal.ONE, BigDecimal.TEN, Partner.TANGO, Platform.AMAZON, "192.168", "key", "nick", "email", "url", BigDecimal.ZERO, new DateTime(), new HashSet<Location>(), new HashSet<String>());
        final String serialize = helper.serialize(session);
        System.out.println(serialize);
        final Session deserialized = helper.deserialize(Session.class, serialize);
        Assert.assertThat(session, CoreMatchers.is(IsEqual.equalTo(deserialized)));
    }
}
