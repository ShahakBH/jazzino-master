package com.yazino.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.IOException;
import java.io.StringWriter;

public class JsonHelper {
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonHelper() {
        mapper.registerModule(new JodaModule());
    }

    public JsonHelper(final boolean skipNullFields) {
        this();
        if (skipNullFields) {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    }

    public String serialize(final Object o) {
        try {
            final StringWriter sw = new StringWriter();
            mapper.writeValue(sw, o);
            return sw.toString();
        } catch (final Exception e) {
            throw new RuntimeException("JSON serialization error ", e);
        }
    }

    /**
     * This method only works for mutable classes with a parameterless constructor
     */
    public <T> T deserialize(final Class<T> clazz, final String s) {
        try {
            return mapper.readValue(s, clazz);
        } catch (final IOException e) {
            throw new RuntimeException("JSON deserialization error ", e);
        }
    }
}
