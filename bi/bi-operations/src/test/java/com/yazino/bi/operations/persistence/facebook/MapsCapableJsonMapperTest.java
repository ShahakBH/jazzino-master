package com.yazino.bi.operations.persistence.facebook;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.restfb.exception.FacebookException;

public class MapsCapableJsonMapperTest {
    private MapsCapableJsonMapper underTest;

    @Before
    public void setUp() {
        underTest = new MapsCapableJsonMapper();
    }

    @Test
    public void shouldCorrectlyMapMaps() {
        // GIVEN a JSON string matching a map
        final String jsonString = "{\"mappedField\":{12:\"Test 1\",17:\"Test 2\"},\"stringField\":\"str\"}";

        // WHEN requesting to map the JSON string
        final TestMappableClass test = underTest.toJavaObject(jsonString, TestMappableClass.class);

        // THEN the result contains all the expected data
        assertEquals("Test 1", test.getMappedField().get(12));
        assertEquals("Test 2", test.getMappedField().get(17));
        assertEquals("str", test.getStringField());
    }

    @Test(expected = FacebookException.class)
    public void shouldRefuseNonParameterizedMaps() {
        // GIVEN a JSON string matching a map
        final String jsonString = "{\"oldStyleMappedField\":{12:\"Test 1\",17:\"Test 2\"}}";

        // WHEN requesting to map the JSON string
        underTest.toJavaObject(jsonString, TestMappableClass.class);

        // THEN the exception is thrown
    }
}
