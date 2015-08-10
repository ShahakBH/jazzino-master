package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfileLoginResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionFactory;
import com.yazino.web.util.ClientContextConverter;
import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * The Yazino login provider for the lobby.
 * <p/>
 * This augments the {@link AuthenticationService} to provide
 * any other actions the web tier requires for login.
 */
@Service
public class YazinoWebLoginService {
    private static final Logger LOG = LoggerFactory.getLogger(YazinoWebLoginService.class);

    private final AuthenticationService authenticationService;
    private final CookieHelper cookieHelper;
    private final RememberMeHandler rememberMeHandler;
    private final LobbySessionFactory lobbySessionFactory;
    private final GameTypeResolver gameTypeResolver;

    @Autowired
    public YazinoWebLoginService(final AuthenticationService authenticationService,
                                 final CookieHelper cookieHelper,
                                 final RememberMeHandler rememberMeHandler,
                                 final LobbySessionFactory lobbySessionFactory,
                                 final GameTypeResolver gameTypeResolver) {
        notNull(authenticationService, "authenticationService may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(rememberMeHandler, "rememberMeHandler may not be null");
        notNull(lobbySessionFactory, "lobbySessionFactory may not be null");
        notNull(gameTypeResolver);

        this.gameTypeResolver = gameTypeResolver;
        this.authenticationService = authenticationService;
        this.cookieHelper = cookieHelper;
        this.rememberMeHandler = rememberMeHandler;
        this.lobbySessionFactory = lobbySessionFactory;
    }

    public LoginResult login(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final String email,
                             final String password,
                             final Platform platform,
                             final Partner partnerId) {
        LOG.debug("Login with email={}, platform={}", email, platform);

        final PlayerProfileLoginResponse loginResponse = authenticationService.loginYazinoUser(email, password);
        if (loginResponse.getLoginResult() == LoginResult.NEW_USER
                || loginResponse.getLoginResult() == LoginResult.EXISTING_USER) {
            loginAuthenticatedUser(request, response, email, loginResponse, loginResponse.getLoginResult(), platform, partnerId);
        }
        return loginResponse.getLoginResult();
    }

    public NewlyRegisteredUserLoginResult loginNewlyRegisteredUser(final HttpServletRequest request,
                                                                   final HttpServletResponse response,
                                                                   final String email,
                                                                   final String password,
                                                                   final Platform platform,
                                                                   final Partner partnerId) {
        final PlayerProfileLoginResponse loginResponse = authenticationService.loginYazinoUser(email, password);
        final LobbySession lobbySession = loginAuthenticatedUser(request, response, email, loginResponse, LoginResult.NEW_USER, platform, partnerId);
        return new NewlyRegisteredUserLoginResult(LoginResult.NEW_USER, lobbySession);
    }

    private LobbySession loginAuthenticatedUser(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final String email,
                                                final PlayerProfileLoginResponse loginResponse,
                                                final LoginResult loginResult,
                                                final Platform platform,
                                                final Partner partnerId) {
        final String gameType = gameTypeResolver.resolveGameType(request, response);
        final LobbySession lobbySession = lobbySessionFactory.registerAuthenticatedSession(
                request, response,
                partnerId,
                loginResponse.getPlayerId(),
                loginResult, true, platform, ClientContextConverter.toMap(""), gameType);

        if (lobbySession != null) {
            rememberMeHandler.storeRememberMeCookie(
                    partnerId, platform, loginResponse.getPlayerId(), email, request, response);
        }
        cookieHelper.setRedirectTo(response, null);
        return lobbySession;
    }

    public static class NewlyRegisteredUserLoginResult {

        private final LoginResult loginResult;
        private final LobbySession lobbySession;

        public NewlyRegisteredUserLoginResult(LoginResult loginResult, LobbySession lobbySession) {
            this.loginResult = loginResult;
            this.lobbySession = lobbySession;
        }

        public LobbySession getLobbySession() {
            return lobbySession;
        }

        public LoginResult getLoginResult() {
            return loginResult;
        }
    }
}
