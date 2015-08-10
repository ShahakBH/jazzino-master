package com.yazino.web.interceptor;

import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.security.ProtectedResourceClassifier;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static org.apache.commons.lang3.Validate.notNull;

public class AuthenticatedUserInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatedUserInterceptor.class);

    private final LobbySessionCache lobbySessionCache;
    private final String loginRedirect;
    private final RememberMeHandler rememberMeHandler;
    private final ProtectedResourceClassifier protectedResourceClassifier;
    private GameTypeResolver gameTypeResolver;

    @Autowired
    public AuthenticatedUserInterceptor(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            final RememberMeHandler rememberMeHandler,
            @Value("${senet.web.redir.for.login}") final String loginRedirect,
            ProtectedResourceClassifier protectedResourceClassifier, final GameTypeResolver gameTypeResolver) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(rememberMeHandler, "rememberMeHandler may not be null");
        notNull(loginRedirect, "loginRedirect may not be null");
        notNull(protectedResourceClassifier, "protectedResourceClassifier may not be null");
        notNull(gameTypeResolver );

        this.gameTypeResolver = gameTypeResolver;
        this.lobbySessionCache = lobbySessionCache;
        this.rememberMeHandler = rememberMeHandler;
        this.loginRedirect = loginRedirect;
        this.protectedResourceClassifier = protectedResourceClassifier;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("entering preHandle with cookies {}", cookiesAsString(request));
        }

        boolean requiresAuthorisation = protectedResourceClassifier.requiresAuthorisation(request.getRequestURI());
        final boolean auth = !requiresAuthorisation || checkAuthenticatedUser(request, response);
        return auth && super.preHandle(request, response, handler);
    }

    private String cookiesAsString(final HttpServletRequest request) {
        final StringBuilder cookieString = new StringBuilder("[");
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieString.length() > 1) {
                    cookieString.append(",");
                }
                cookieString.append("{")
                        .append(cookie.getName())
                        .append(":")
                        .append(cookie.getValue())
                        .append(":")
                        .append(cookie.getDomain())
                        .append("}");
            }
        }
        cookieString.append("]");
        return cookieString.toString();
    }

    private boolean checkAuthenticatedUser(final HttpServletRequest request,
                                           final HttpServletResponse response) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("entering checkAuthenticatedUser {}", ToStringBuilder.reflectionToString(request.getCookies()));
        }

        boolean requiresAuthorisation = protectedResourceClassifier.requiresAuthorisation(request.getRequestURI());

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        String gameType = gameTypeResolver.resolveGameType(request, response);
        if (lobbySession == null) {
            if (rememberMeHandler.attemptAutoLogin(request, response, new HashMap<String, Object>(), gameType) == null && requiresAuthorisation) {
                return redirectUnauthenticatedUser(request, response);
            }
        } else if (requiresAuthorisation && !lobbySessionCache.hasSessionRequestHeaderOrCookie(request)) {
            response.sendRedirect(getNoCookiesUrl());
            return false;
        }
        return true;
    }


    private boolean redirectUnauthenticatedUser(final HttpServletRequest request,
                                                final HttpServletResponse response)
            throws IOException {
        final String from = request.getRequestURI();
        if (!clientAcceptsHTML(request)) {
            LOG.debug("Client does not accept HTML; returning unauthorised response for query: {}", from);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        final String redirectString = redirectionUrlFor(request, from);
        LOG.debug("Redirecting unauthorised client from: {}; to: {}", from, redirectString);
        response.sendRedirect(redirectString);

        return false;
    }

    private String redirectionUrlFor(final HttpServletRequest request, String from) {
        if (from == null) {
            from = "";
        }
        String qs = request.getQueryString();
        if (qs == null) {
            qs = "";
        } else {
            qs = "&" + qs;
        }

        final String baseRedirect = "/" + loginRedirect;
        return String.format("%s?from=%s%s", baseRedirect, from, qs);
    }

    private boolean clientAcceptsHTML(final HttpServletRequest request) {
        final String acceptHeader = request.getHeader("Accept");
        return acceptHeader == null || acceptHeader.contains("text/html") || acceptHeader.contains("*/*");
    }

    private String getNoCookiesUrl() {
        return "/noCookies";
    }
}
