package com.yazino.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.restfb.FacebookClient;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.domain.facebook.LegacyEngagementData;
import com.yazino.web.session.ReferrerSessionCache;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

/**
 * Class to handle the processing of the Legacy Engagement request.
 * i.e. Data: {Engagement: {tracking=tracking data}}
 */

@Deprecated
@Service("legacyEngagementAppToUserRequestProcessor")
public class LegacyEngagementAppToUserRequestProcessor implements FacebookAppToUserRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyEngagementData.class);
    private final ReferrerSessionCache referrerSessionCache;

    @Autowired
    public LegacyEngagementAppToUserRequestProcessor(final ReferrerSessionCache referrerSessionCache) {
        notNull(referrerSessionCache);
        this.referrerSessionCache = referrerSessionCache;
    }

    @Override
    public void processFacebookAppToUserRequest(final List<FacebookAppToUserRequest> legacyAppToUserRequestResponseList,
                                                final FacebookClient fbClient) {


        DateTime mostRecentCreateDate = new DateTime(0L);

        String trackingRef = null;
        for (FacebookAppToUserRequest legacyFacebookAppToUserRequest : legacyAppToUserRequestResponseList) {
            if (legacyFacebookAppToUserRequest.getCreatedTime().isAfter(mostRecentCreateDate)
                    && legacyFacebookAppToUserRequest.getData() != null) {

                final ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JodaModule());

                LegacyEngagementData legacyEngagementData = null;
                try {
                    legacyEngagementData = objectMapper.readValue(
                            legacyFacebookAppToUserRequest.getData(), LegacyEngagementData.class);
                    trackingRef = legacyEngagementData.getEngagement().get(LegacyEngagementData.TRACKING_KEY);
                } catch (IOException e) {
                    LOG.warn("Could not process Legacy Engagement data String {}",
                            legacyEngagementData.getEngagement(), e);

                }
                mostRecentCreateDate = legacyFacebookAppToUserRequest.getCreatedTime();
            }
            fbClient.deleteObject(legacyFacebookAppToUserRequest.getId());
        }

        if (!StringUtils.isBlank(trackingRef)) {
            referrerSessionCache.setReferrer(trackingRef);
        }

    }

}
