package com.yazino.web.api;

import com.yazino.platform.Platform;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.ClientPropertyService;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.LobbySessionReference;
import com.yazino.web.util.ClientContextConverter;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Controller
@AllowPublicAccess("/api/*/configuration/**")
public class ClientConfigurationController {

    private final ClientPropertyService clientPropertyService;
    private final LobbySessionCache lobbySessionCache;
    private final RememberMeHandler rememberMeHandler;
    private final WebApiResponses webApiResponses;
    private final CookieHelper cookieHelper;
    private GameTypeResolver gameTypeResolver;

    @Autowired
    public ClientConfigurationController(ClientPropertyService clientPropertyService,
                                         LobbySessionCache lobbySessionCache,
                                         RememberMeHandler rememberMeHandler,
                                         WebApiResponses webApiResponses, final CookieHelper cookieHelper,
                                         final GameTypeResolver gameTypeResolver) {
        this.clientPropertyService = clientPropertyService;
        this.lobbySessionCache = lobbySessionCache;
        this.rememberMeHandler = rememberMeHandler;
        this.webApiResponses = webApiResponses;
        this.cookieHelper = cookieHelper;
        this.gameTypeResolver = gameTypeResolver;
    }

    /**
     * Loads configuration properties. If a session is currently active, session related properties are loaded.
     */
    @RequestMapping("/api/1.0/configuration/{platform}/{game-type}")
    public void getConfiguration_v1_0(HttpServletRequest request,
                                      HttpServletResponse response,
                                      @PathVariable("platform") String platformValue,
                                      @PathVariable("game-type") String gameType) throws IOException {
        Platform platform = parsePlatform(platformValue);
        Map<String, Object> properties = buildConfiguration_v1_0(request, platform);
        webApiResponses.writeOk(response, properties);
    }

    private Map<String, Object> buildConfiguration_v1_0(HttpServletRequest request,
                                                        Platform platform) {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(clientPropertyService.getBasePropertiesFor(platform));

        getSessionProperties(request, properties);
        return properties;
    }

    private void getSessionProperties(final HttpServletRequest request, final Map<String, Object> properties) {
        Map<String, Object> sessionProperties = new HashMap<>();
        LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session != null) {
            sessionProperties.putAll(clientPropertyService.getSessionPropertiesFor(session));
            sessionProperties.put("session", new LobbySessionReference(session).encode());
        }
        properties.putAll(sessionProperties);
    }

    /**
     * Loads configuration properties. If a session is currently active, session related properties are loaded.
     */
    @RequestMapping("/api/1.1/configuration/{platform}/{game-type}/{client-id}")
    public void getConfiguration_v1_1(HttpServletRequest request,
                                      HttpServletResponse response,
                                      @PathVariable("platform") String platformValue,
                                      @PathVariable("game-type") String gameType,
                                      @PathVariable("client-id") String clientId,
                                      @RequestParam(defaultValue = "false") boolean useRememberMe,
                                      @RequestParam(required = false) final String clientContext) throws IOException {
        Platform platform = parsePlatform(platformValue);
        attemptAutoLoginOnRememberMe(request, response, useRememberMe, ClientContextConverter.toMap(clientContext), gameType);

        try {
            Map<String, Object> properties = buildConfiguration_v1_1(request, platform, gameType, clientId);
            webApiResponses.writeOk(response, properties);

        } catch (IllegalStateException e) {
            webApiResponses.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private Map<String, Object> buildConfiguration_v1_1(HttpServletRequest request,
                                                        Platform platform,
                                                        final String gameType,
                                                        final String clientId) {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(clientPropertyService.getBasePropertiesFor(platform));
        getSessionProperties(request, properties);
        addVersionInfo(gameType, clientId, platform, properties);
        addMaintenanceAlerts(gameType, properties);
        addFacebookCanvasFlags(request, properties);
        return properties;
    }


    private void addVersionInfo(String gameType, String clientId, Platform platform, Map<String, Object> properties) {
        properties.putAll(clientPropertyService.getVersionsFor(platform, gameType, clientId));
    }

    private void addMaintenanceAlerts(String gameType, Map<String, Object> properties) {
        properties.putAll(clientPropertyService.getAvailabilityOfGameType(gameType));
    }

    private void addFacebookCanvasFlags(HttpServletRequest request, Map<String, Object> properties) {
        final boolean onCanvas = cookieHelper.isOnCanvas(request);
        properties.put("facebook-canvas", Boolean.toString(onCanvas));
    }

    private Map<String, Object> getSessionProperties(HttpServletRequest request,
                                                     HttpServletResponse response,
                                                     final LobbySession session,
                                                     boolean useRememberMe,
                                                     final Map<String, Object> clientContextMap,
                                                     final String gameType) {
        Map<String, Object> sessionProperties = new HashMap<>();
        attemptAutoLoginOnRememberMe(request, response, useRememberMe, clientContextMap, gameType);
        if (session != null) {
            sessionProperties.putAll(clientPropertyService.getSessionPropertiesFor(session));
            sessionProperties.put("session", new LobbySessionReference(session).encode());
        }
        return sessionProperties;
    }

    private void attemptAutoLoginOnRememberMe(final HttpServletRequest request, final HttpServletResponse response,
                                              final boolean useRememberMe, final Map<String, Object> clientContextMap,
                                              final String gameType) {
        if (useRememberMe) {
            useRememberMeToAuthenticateIfPossible(request, response, clientContextMap, gameType);
        }
    }

    /**
     * Loads session-related properties.
     */
    @Deprecated
    @RequestMapping("/api/*/configuration/session")
    public void getSessionConfiguration(HttpServletRequest request,
                                        HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            webApiResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, Collections.<String, Object>emptyMap());
            return;
        }
        final String gameType = gameTypeResolver.resolveGameType(request, response);

        Map<String, Object> properties = getSessionProperties(request, response, session, false, ClientContextConverter.toMap(null),
                                                              gameType);
        webApiResponses.writeOk(response, properties);
    }

    private Platform parsePlatform(String platformValue) {
        Platform platform = Platform.safeValueOf(platformValue);
        checkArgument(platform != null, "invalid platform value \"" + platformValue + "\"");
        return platform;
    }

    private void useRememberMeToAuthenticateIfPossible(HttpServletRequest request, HttpServletResponse response,
                                                       final Map<String, Object> clientContextMap,
                                                       final String gameType) {
        if (lobbySessionCache.getActiveSession(request) == null) {
            rememberMeHandler.attemptAutoLogin(request, response, clientContextMap,gameType);
        }
    }

}
