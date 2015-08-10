package com.yazino.metrics.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public final class JodaSupportingObjectMapperFactory {

    public JodaSupportingObjectMapperFactory() {

    }

    public ObjectMapper newMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

}
