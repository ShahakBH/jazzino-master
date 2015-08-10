package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.tracking.TrackingEvent;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Controller
public class TrackingController {

    private static final Logger LOG = LoggerFactory.getLogger(TrackingController.class);

    private final LobbySessionCache lobbySessionCache;
    private final QueuePublishingService queuePublishingService;
    private final JsonHelper jsonHelper = new JsonHelper();

    @Autowired
    public TrackingController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                              @Qualifier("trackingEventPublishingService") QueuePublishingService queuePublishingService) {
        this.lobbySessionCache = lobbySessionCache;
        this.queuePublishingService = queuePublishingService;
    }

    @RequestMapping(value = "/tracking/event", method = RequestMethod.POST)
    @AllowPublicAccess
    public void trackEvent(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestParam("name") String name) {

        checkArgument(!StringUtils.isBlank(name), "name is not specified");
        LobbySession lobbySession = lobbySessionCache.getActiveSession(request);

        if (lobbySession == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        Platform platform = lobbySession.getPlatform();
        checkArgument(platform != null); // checkNotNull generates wrong exception
        BigDecimal playerId = lobbySession.getPlayerId();
        checkArgument(playerId != null);

        Map<String, String> properties;
        String serializedProperties = null;
        try {
            serializedProperties = safeReadProperties(request);
            properties = asPropertyMap(serializedProperties);
        } catch (Exception e) {
            LOG.debug("Unable to deserialize properties: {}", serializedProperties, e);
            safeSendError(response, HttpStatus.BAD_REQUEST);
            return;
        }

        boolean successful = safeTrackEvent(platform, playerId, name, properties);

        if (successful) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        } else {
            safeSendError(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void safeSendError(HttpServletResponse response, HttpStatus status) {
        try {
            response.sendError(status.value());
        } catch (IOException e) {
            response.setStatus(status.value());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> asPropertyMap(String json) {
        Map<String, Object> map = jsonHelper.deserialize(HashMap.class, json);
        Map<String, String> propertyMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = null;
            if (entry.getValue() != null) {
                value = entry.getValue().toString();
            }
            propertyMap.put(entry.getKey(), value);
        }
        return propertyMap;
    }

    private String safeReadProperties(HttpServletRequest request) {
        try {
            return IOUtils.toString(request.getInputStream());
        } catch (Exception e) {
            LOG.warn("Unable to read event properties from input stream", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean safeTrackEvent(Platform platform, BigDecimal playerId, String name, Map<String, String> properties) {
        try {
            queuePublishingService.send(new TrackingEvent(platform, playerId, name, properties, new DateTime()));
            return true;
        } catch (Exception e) {
            LOG.warn("Unable to track event (platform={}, playerId={}, name={})", platform, playerId, name, e);
            return false;
        }
    }
}
