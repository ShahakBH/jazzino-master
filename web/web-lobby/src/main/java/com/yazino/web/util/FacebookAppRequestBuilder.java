package com.yazino.web.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import org.joda.time.DateTime;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;
import strata.server.lobby.api.facebook.FacebookDataContainer;
import strata.server.lobby.api.facebook.FacebookDataContainerBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class FacebookAppRequestBuilder {
    private String id;
    private String data;
    private String message;
    private DateTime createdTime;

    public FacebookAppRequestBuilder withId(final String value) {
        this.id = value;
        return this;
    }
    public FacebookAppRequestBuilder withData(final String value) {
        this.data = value;
        return this;
    }
    public FacebookAppRequestBuilder withMessage(final String value) {
        this.message = value;
        return this;
    }
    public FacebookAppRequestBuilder withCreatedTime(final DateTime value) {
        this.createdTime = value;
        return this;
    }
    public FacebookAppRequestBuilder withEngagementData(final String tracking,
                                                        final List<String> actions) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final FacebookDataContainer facebookDataContainer = new FacebookDataContainerBuilder().build();

        final HashMap<String, Object> trackingMap = new HashMap<String, Object>();

        trackingMap.put("ref", tracking);
        facebookDataContainer.setTracking(trackingMap);
        facebookDataContainer.setActions(actions);
        facebookDataContainer.setType(FacebookAppToUserRequestType.Engagement);
        this.data = objectMapper.writeValueAsString(facebookDataContainer);
        return this;
    }


    public FacebookAppToUserRequest build()   {
        return new FacebookAppToUserRequest(id, data, message, createdTime);
    }
}
