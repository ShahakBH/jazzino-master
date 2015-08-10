package com.yazino.web.domain.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;

public class LegacyFacebookAppRequestBuilder {
    private String id;
    private String data;
    private String message;
    private DateTime createdTime;

    public LegacyFacebookAppRequestBuilder withId(final String newId) {
        this.id = newId;
        return this;
    }

    public LegacyFacebookAppRequestBuilder withData(final String newData) {
        this.data = newData;
        return this;
    }

    public LegacyFacebookAppRequestBuilder withMessage(final String newMessage) {
        this.message = newMessage;
        return this;
    }

    public LegacyFacebookAppRequestBuilder withCreatedTime(final DateTime newCreatedTime) {
        this.createdTime = newCreatedTime;
        return this;
    }

    public LegacyFacebookAppRequestBuilder withEngagementData(final String tracking) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());

        final HashMap<String, String> trackingMap = new HashMap<>();

        trackingMap.put("tracking", tracking);

        this.data = mapper.writeValueAsString(new LegacyEngagementData(trackingMap));

        return this;
    }


    public FacebookAppToUserRequest build() {
        return new FacebookAppToUserRequest(id, data, message, createdTime);
    }
}
