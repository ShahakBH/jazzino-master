package com.yazino.web.domain.world;

import com.yazino.web.util.JsonHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TangoLoginJsonTest {
    JsonHelper jsonHelper = new JsonHelper();
    private TangoLoginJson underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new TangoLoginJson();
        underTest.setDisplayName("Jim");
        underTest.setAvatarUrl("http://your.mum/so/fat.gif");
        underTest.setAccountId("123abc");


    }

    @Test
    public void jsonShouldDeserialise() {
        Assert.assertThat(jsonHelper.deserialize(TangoLoginJson.class,
                "{\"displayName\":\"Jim\",\"accountId\":\"123abc\",\"avatarUrl\":\"http://your.mum/so/fat.gif\"}"),
                is(equalTo(underTest)));

    }

    @Test
    public void jsonShouldSerialise() {
        final String serialized = jsonHelper.serialize(underTest);
        System.out.println(serialized);
        assertThat(jsonHelper.deserialize(TangoLoginJson.class, serialized), is(equalTo(underTest)));

    }

}
