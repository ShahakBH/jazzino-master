package com.yazino.web.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;

@Service("jsonHelper")
public class JsonHelper {
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonHelper() {
        mapper.registerModule(new JodaModule());
    }

    public String serialize(final Object o) {
        try {
            return mapper.writeValueAsString(o);
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

    /**
     * This method only works for mutable classes with a parameterless constructor
     */
    public <T> T deserialize(final Class<T> clazz, final Reader reader) {
        try {
            return mapper.readValue(reader, clazz);
        } catch (final IOException e) {
            throw new RuntimeException("JSON deserialization error ", e);
        }
    }
}
