package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.android.MessagingDeviceRegistrationEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

@Controller
public class MessagingDeviceRegistrationController {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingDeviceRegistrationController.class);
    private final LobbySessionCache lobbySessionCache;
    private final QueuePublishingService<MessagingDeviceRegistrationEvent> deviceRegistrationService;
    private final WebApiResponses responseWriter;

    @Autowired
    public MessagingDeviceRegistrationController(LobbySessionCache lobbySessionCache,
                                                 @Qualifier("messagingDeviceRegistrationService")
                                                 QueuePublishingService<MessagingDeviceRegistrationEvent> deviceRegistrationService,
                                                 final WebApiResponses responseWriter) {
        this.lobbySessionCache = lobbySessionCache;
        this.deviceRegistrationService = deviceRegistrationService;
        this.responseWriter = responseWriter;
    }

    @RequestMapping(value = "/api/1.1/message/registration", method = RequestMethod.POST)
    public void storeDeviceAndRegistrationId(HttpServletRequest request,
                                             HttpServletResponse response,
                                             @RequestParam("gameType") String gameType,
                                             @RequestParam("appId") String appId,
                                             @RequestParam("deviceId") String deviceId,
                                             @RequestParam("pushToken") String pushToken) throws IOException {

        LobbySession session = lobbySessionCache.getActiveSession(request);

        if (session == null) {
            LOG.debug("storeDeviceAndRegistrationId could not load session for player {}", request);
            responseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(), "no session");
        }

        try {
            MessagingDeviceRegistrationEvent event = new MessagingDeviceRegistrationEvent(session.getPlayerId(), gameType, pushToken, session.getPlatform());
            event.setAppId(appId);
            event.setDeviceId(deviceId);
            deviceRegistrationService.send(event);
        } catch (Exception e) {
            LOG.error("problem with sending MessageDeviceRegistrationEvent to Queue");
            responseWriter.writeError(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), "could not add registration message to queue, please try again later.");
        }
    }

    @RequestMapping(value = "/api/1.0/message/registration", method = RequestMethod.POST)
    public void storeRegistrationId(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    @RequestParam("gameType") String gameType,
                                    @RequestParam("registrationId") final String registrationId) throws IOException {

        LobbySession session = lobbySessionCache.getActiveSession(request);

        if (session == null) {
            LOG.debug("storeRegistrationId could not load session for player {}", request);
            responseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(), "no session");
        }

        try {
            deviceRegistrationService.send(new MessagingDeviceRegistrationEvent(session.getPlayerId(), gameType, registrationId, session.getPlatform()));
        } catch (Exception e) {
            LOG.error("problem with sending MessageDeviceRegistrationEvent to Queue");
            responseWriter.writeError(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), "could not add registration message to queue, please try again later.");
        }
    }

    /**
     * @Deprecated("Only for older Android clients new clients should use storeRegistrationId")
     */
    @RequestMapping(value = "/google-cloud-messaging/registered-devices", method = RequestMethod.PUT)
    @AllowPublicAccess
    public void storeGCMRegistrationId(@RequestParam("playerId") BigDecimal playerId,
                                       @RequestParam("gameType") String gameType,
                                       @RequestParam("registrationId") String registrationId,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws IOException {

        checkArgument(playerId != null, "playerId not specified");
        checkArgument(StringUtils.trimToNull(gameType) != null, "game-type not specified");
        checkArgument(StringUtils.trimToNull(registrationId) != null, "registration-id not specified");

        handleSafely(playerId, gameType, registrationId, request, response, Platform.ANDROID);
    }

    private void handleSafely(BigDecimal playerId,
                              String gameType,
                              String registrationId,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              final Platform platform) {
        try {
            handle(playerId, gameType, registrationId, request, response, platform);
        } catch (Exception e) {
            LOG.error("Unable to update register device (playerId={}, gameType={}, registrationId={}, platform={})",
                    playerId, gameType, registrationId, platform, e);
        }
    }

    private void handle(BigDecimal playerId,
                        String gameType,
                        String registrationId,
                        HttpServletRequest request,
                        HttpServletResponse response, final Platform platform) throws IOException {

        LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        if (activeSession == null || !nullSafeEquals(activeSession.getPlayerId(), playerId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        deviceRegistrationService.send(new MessagingDeviceRegistrationEvent(playerId, gameType, registrationId, platform));
    }
}
