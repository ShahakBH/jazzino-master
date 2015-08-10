package com.yazino.web.util;

import com.yazino.platform.session.SessionClientContextKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class ClientContextConverterTest {

    private static final String CLIENT_CONTEXT = format("{\"%s\":\"unique identifier for device\"}", SessionClientContextKey.DEVICE_ID.name());

    @Test
    public void toMapShouldReturnAnEmptyMapIfStringIsNull() {

        Map<String, Object> actual = ClientContextConverter.toMap(null);
        Map<String, Object> expected = new HashMap<>();

        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void toMapShouldReturnAnEmptyMapIfStringIsNullOrEmptyString() {
        Map<String, Object> actual = ClientContextConverter.toMap("");
        Map<String, Object> expected = new HashMap<>();

        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void toMapShouldReturnAnEmptyMapIfEmptyJsonString() {
        Map<String, Object> actual = ClientContextConverter.toMap("{}");
        Map<String, Object> expected = new HashMap<>();

        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void toMapShouldReturnEmptyMapIfExceptionIsThrown()  {
        Map<String, Object> actual = ClientContextConverter.toMap("who needs json i will do my own thang");
        Map<String, Object> expected = new HashMap<>();

        Assert.assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void toMapShouldReturnMapFromJsonString(){
        Map<String, Object> actual = ClientContextConverter.toMap(CLIENT_CONTEXT);
        Map<String, Object> expected = new HashMap<>();
        expected.put(SessionClientContextKey.DEVICE_ID.name(), "unique identifier for device");

        Assert.assertThat(actual, is(equalTo(expected)));
    }

}
