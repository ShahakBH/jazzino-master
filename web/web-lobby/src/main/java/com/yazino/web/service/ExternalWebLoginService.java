package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfileLoginResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionFactory;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * The external login provider (Facebook, etc.) for the lobby.
 * <p/>
 * This augments the {@link AuthenticationService} to provide
 * any other actions the web tier requires for login.
 */
@Service
public class ExternalWebLoginService {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalWebLoginService.class);

    private final AuthenticationService authenticationService;
    private final CookieHelper cookieHelper;
    private final ReferrerSessionCache referrerSessionCache;
    private final LobbySessionFactory lobbySessionFactory;

    @Autowired
    public ExternalWebLoginService(final AuthenticationService authenticationService,
                                   final CookieHelper cookieHelper,
                                   final ReferrerSessionCache referrerSessionCache,
                                   final LobbySessionFactory lobbySessionFactory) {
        notNull(authenticationService, "authenticationService may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(referrerSessionCache, "referrerSessionCache may not be null");
        notNull(lobbySessionFactory, "lobbySessionFactory may not be null");

        this.authenticationService = authenticationService;
        this.cookieHelper = cookieHelper;
        this.referrerSessionCache = referrerSessionCache;
        this.lobbySessionFactory = lobbySessionFactory;
    }

    public LoginResponse login(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final Partner partnerId,
                               final String gameType,
                               final PlayerInformationHolder provider,
                               final boolean useSessionCookie,
                               Platform platform,
                               final Map<String, Object> clientContext) {
        notNull(request, "request is null");
        notNull(response, "response is null");
        notNull(partnerId, "partnerId is null");
        notBlank(gameType, "gameType is null");
        notNull(provider, "provider is null");

        final String referrer = referrerSessionCache.getReferrer();

        final PlayerProfileLoginResponse loginResponse = authenticationService.loginExternalUser(
                request.getRemoteAddr(), partnerId, provider, referrer, platform, gameType);

        LoginResult loginResult = loginResponse.getLoginResult();
        if (loginResult == LoginResult.NEW_USER) {
            cookieHelper.setIsNewPlayer(response);
        }

        if (loginResult == LoginResult.NEW_USER || loginResult == LoginResult.EXISTING_USER) {

            LobbySession session = lobbySessionFactory.registerAuthenticatedSession(request, response, partnerId,
                    loginResponse.getPlayerId(), loginResult, useSessionCookie, platform, clientContext, gameType);

            if (session == null) {
                LOG.warn("Session was null for {} player {}", loginResult.name(), loginResponse.getPlayerId());
                return new LoginResponse(LoginResult.FAILURE);
            }

            cookieHelper.setOnCanvas(response, platform == FACEBOOK_CANVAS);

            if (provider.getPlayerProfile() != null) {
                provider.getPlayerProfile().setPlayerId(loginResponse.getPlayerId());
            }

            return new LoginResponse(loginResult, session);
        }

        return new LoginResponse(loginResult);
    }
}
