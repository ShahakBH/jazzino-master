package com.yazino.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookAppToUserRequestType.Engagement;
import static strata.server.lobby.api.facebook.FacebookAppToUserRequestType.LegacyEngagement;


@Service
public class FacebookAppToUserRequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookAppToUserRequestHandler.class);

    private static final String UTF_8 = "UTF-8";
    static final String REFER_FRIEND_PATTERN = "\"data\":\\s*\"\\d+\"";
    static final String ENGAGEMENT_PATTERN = "\"data\":\\s*\"\\{.*\"type\\\\\":\\\\\"Engagement\\\\\"";
    static final String LEGACY_ENGAGEMENT_PATTERN = "\"data\":\\s*\"\\{\\\\\"Engagement\\\\\":";
    private final FacebookClientFactory fbClientFactory;
    private final Pattern referFriendPattern = Pattern.compile(REFER_FRIEND_PATTERN);
    private final Pattern engagementPattern = Pattern.compile(ENGAGEMENT_PATTERN);
    private final Pattern legacyEngagementPattern = Pattern.compile(LEGACY_ENGAGEMENT_PATTERN);

    @Resource(name = "facebookAppToUserRequestProcessors")
    private Map<FacebookAppToUserRequestType, FacebookAppToUserRequestProcessor> appToUserRequestProcessorMap;

    @Autowired
    public FacebookAppToUserRequestHandler(final FacebookClientFactory fbClientFactory) {
        notNull(fbClientFactory, "Facebook Client Factory Should not be Null");
        this.fbClientFactory = fbClientFactory;
    }

    public void processAppRequests(final String encodedAppRequestIds, final String accessToken) {
        if (!isValidParameters(encodedAppRequestIds, accessToken)) {
            return;
        }

        final List<BatchRequest> batchRequestList = generateBatchRequestList(encodedAppRequestIds);

        final FacebookClient fbClient = fbClientFactory.getClient(accessToken);
        final List<BatchResponse> batchResponseList =
                fbClient.executeBatch(batchRequestList.toArray(new BatchRequest[batchRequestList.size()]));

        final Map<String, List<FacebookAppToUserRequest>> appToUserRequests =
                convertBatchRequestsToFacebookAppToUserRequests(batchResponseList);

        appToUserRequestProcessorMap.get(Engagement).processFacebookAppToUserRequest(
                appToUserRequests.get(Engagement.name()), fbClient);
        appToUserRequestProcessorMap.get(LegacyEngagement).processFacebookAppToUserRequest(
                appToUserRequests.get(LegacyEngagement.name()), fbClient);
    }

    private Map<String, List<FacebookAppToUserRequest>> convertBatchRequestsToFacebookAppToUserRequests(
            final List<BatchResponse> batchResponseList) {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        final List<FacebookAppToUserRequest> facebookAppToUserRequests
                = new ArrayList<>();
        final List<FacebookAppToUserRequest> legacyEngagementAppToUserRequestList
                = new ArrayList<>();
        for (BatchResponse batchResponse : batchResponseList) {

            if (batchResponse != null && batchResponse.getCode() == HttpServletResponse.SC_OK) {
                final String appRequestJson = batchResponse.getBody();

                try {
                    final Matcher referFriendMatcher = referFriendPattern.matcher(appRequestJson);
                    final Matcher engagementMatcher = engagementPattern.matcher(appRequestJson);
                    final Matcher legacyEngagementMatcher = legacyEngagementPattern.matcher(appRequestJson);

                    if (referFriendMatcher.find()) {
                        LOG.info("refer a friend request: do nothing");
                    } else if (engagementMatcher.find()) {
                        final FacebookAppToUserRequest facebookAppToUserRequest
                                = objectMapper.readValue(appRequestJson, FacebookAppToUserRequest.class);
                        facebookAppToUserRequests.add(facebookAppToUserRequest);
                    } else if (legacyEngagementMatcher.find()) {
                        final FacebookAppToUserRequest legacyFacebookAppToUserRequest;
                        legacyFacebookAppToUserRequest = objectMapper.readValue(
                                appRequestJson, FacebookAppToUserRequest.class);
                        legacyEngagementAppToUserRequestList.add(legacyFacebookAppToUserRequest);

                    } else {
                        LOG.warn("Unknown AppRequest: {}", appRequestJson);
                    }

                } catch (IOException e) {
                    LOG.warn("Invalid JSON for AppRequest {}", appRequestJson, e);
                }
            }
        }

        final Map<String, List<FacebookAppToUserRequest>> appRequestList =
                new HashMap<>();

        appRequestList.put(Engagement.name(), facebookAppToUserRequests);
        appRequestList.put("LegacyEngagement", legacyEngagementAppToUserRequestList);

        return appRequestList;
    }

    private List<BatchRequest> generateBatchRequestList(final String encodedAppRequestIds) {
        final String decodedRequestIds = decodeAppRequestIds(encodedAppRequestIds);
        final String[] requestIdList = decodedRequestIds.split(",");

        final List<BatchRequest> batchRequestList = new ArrayList<>();
        for (String requestId : requestIdList) {
            batchRequestList.add(new BatchRequest.BatchRequestBuilder(requestId).build());
        }
        return batchRequestList;
    }

    private String decodeAppRequestIds(final String encodedAppRequestIds) {
        try {
            return URLDecoder.decode(encodedAppRequestIds, UTF_8);
        } catch (UnsupportedEncodingException e) {
            LOG.error("couldn't decode request ID string", e);
            return "";
        }

    }

    private boolean isValidParameters(final String encodedAppRequestIds, final String accessToken) {
        return (StringUtils.isNotBlank(encodedAppRequestIds) && StringUtils.isNotBlank(accessToken));
    }

    public void setAppToUserRequestProcessorMap(
            final Map<FacebookAppToUserRequestType, FacebookAppToUserRequestProcessor> appToUserRequestProcessorMap) {
        this.appToUserRequestProcessorMap = appToUserRequestProcessorMap;
    }
}
