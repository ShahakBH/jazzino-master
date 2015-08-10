package com.yazino.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.restfb.FacebookClient;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.session.ReferrerSessionCache;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookDataContainer;

import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Class to handle all the tracking for FacebookAppRequests
 */

@Service("engagementAppToUserRequestProcessor")
public class EngagementAppToUserRequestProcessor implements FacebookAppToUserRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(EngagementAppToUserRequestProcessor.class);

    private final ReferrerSessionCache referrerSessionCache;

    @Autowired
    public EngagementAppToUserRequestProcessor(final ReferrerSessionCache referrerSessionCache) {
        notNull(referrerSessionCache);
        this.referrerSessionCache = referrerSessionCache;
    }

    @Override
    public void processFacebookAppToUserRequest(final List<FacebookAppToUserRequest> facebookAppToUserRequests,
                                                final FacebookClient fbClient) {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        DateTime mostRecentCreateDate = new DateTime(0L);

        String trackingRef = null;
        for (FacebookAppToUserRequest facebookAppToUserRequest : facebookAppToUserRequests) {
            if (facebookAppToUserRequest.getCreatedTime().isAfter(mostRecentCreateDate)
                    && facebookAppToUserRequest.getData() != null) {
                mostRecentCreateDate = facebookAppToUserRequest.getCreatedTime();
                try {
                    final FacebookDataContainer trackingData = objectMapper.readValue(
                            facebookAppToUserRequest.getData(),
                            FacebookDataContainer.class);
                    trackingRef = (String) trackingData.getTracking().get(FacebookDataContainer.TRACKING_REF_DATA_KEY);
                } catch (IOException e) {
                    LOG.warn("Could not process Engagement data String {}", facebookAppToUserRequest.getData(), e);
                }
            }
            fbClient.deleteObject(facebookAppToUserRequest.getId());
        }

        if (!StringUtils.isBlank(trackingRef)) {
            referrerSessionCache.setReferrer(trackingRef);
        }
    }


}
