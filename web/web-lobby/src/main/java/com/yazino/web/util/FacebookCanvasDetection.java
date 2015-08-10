package com.yazino.web.util;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@Component
public class FacebookCanvasDetection {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookCanvasDetection.class);
    private final FacebookConfiguration facebookConfiguration;
    private final CookieHelper cookieHelper;
    private final GameTypeResolver gameTypeResolver;
    private final YazinoConfiguration yazinoConfiguration;
    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public FacebookCanvasDetection(FacebookConfiguration facebookConfiguration,
                                   CookieHelper cookieHelper,
                                   GameTypeResolver gameTypeResolver,
                                   YazinoConfiguration yazinoConfiguration,
                                   LobbySessionCache lobbySessionCache) {
        this.facebookConfiguration = facebookConfiguration;
        this.cookieHelper = cookieHelper;
        this.gameTypeResolver = gameTypeResolver;
        this.yazinoConfiguration = yazinoConfiguration;
        this.lobbySessionCache = lobbySessionCache;
    }

    public boolean isOnCanvas(final HttpServletRequest request) {
        final boolean cookieOrParameterPresent = cookieHelper.isOnCanvas(request);
        final boolean hasActiveSession = hasActiveSession(request);
        final boolean sessionOverrideDisabled = sessionOverrideDisabled();
        final boolean onCanvas = cookieOrParameterPresent && (sessionOverrideDisabled || hasActiveSession);
        LOG.debug("Is on canvas? {} (param/cookie present = {}, session present = {}, session override disabled = {}", onCanvas, cookieOrParameterPresent, sessionOverrideDisabled, hasActiveSession);
        return onCanvas;
    }

    public boolean redirectionEnabled() {
        return yazinoConfiguration.getBoolean("facebook.canvas-detection.user-redirection", true);
    }

    public ModelAndView createRedirection(HttpServletRequest request, HttpServletResponse response) {
        final String gameType = gameTypeResolver.resolveGameType(request, response);
        if (gameType == null) {
            LOG.warn("Game type could not be resolved. Returning 'home'");
            return new ModelAndView("home");
        }
        final FacebookAppConfiguration config = facebookConfiguration.getAppConfigFor(gameType, CANVAS, LOOSE);
        if (config == null) {
            LOG.warn("Facebook App Config could not be resolved. Returning 'home'");
            return new ModelAndView("home");
        }
        LOG.debug("Creating redirection to {}", config.getAppName());
        return new ModelAndView("fbredirect/secureCanvasRedirect").addObject("appName", config.getAppName());
    }

    private boolean hasActiveSession(HttpServletRequest request) {
        return lobbySessionCache.getActiveSession(request) != null;
    }

    private boolean sessionOverrideDisabled() {
        return !yazinoConfiguration.getBoolean("facebook.canvas-detection.requires-session", true);
    }

}
