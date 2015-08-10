package com.yazino.platform.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

public class Jackson2JodaJsonMessageConverter extends Jackson2JsonMessageConverter {

    @Override
    protected void initializeJsonObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        setJsonObjectMapper(objectMapper);

        super.initializeJsonObjectMapper();
    }
}
