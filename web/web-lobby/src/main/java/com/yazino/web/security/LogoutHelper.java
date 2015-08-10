package com.yazino.web.security;

import com.yazino.platform.session.SessionService;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.apache.commons.lang3.Validate.notNull;

@Service("logoutHelper")
public class LogoutHelper {
    private final LobbySessionCache lobbySessionCache;
    private final ReferrerSessionCache referrerSessionCache;
    private final SessionService sessionService;
    private final RememberMeHandler rememberMeHandler;
    private final CookieHelper cookieHelper;

    @Autowired
    public LogoutHelper(@Qualifier("sessionService") final SessionService sessionService,
                        @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                        @Qualifier("rememberMeHandler") final RememberMeHandler rememberMeHandler,
                        final CookieHelper cookieHelper,
                        final ReferrerSessionCache referrerSessionCache) {
        notNull(sessionService, "sessionService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(referrerSessionCache, "referrerSessionCache may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.sessionService = sessionService;
        this.rememberMeHandler = rememberMeHandler;
        this.cookieHelper = cookieHelper;
        this.referrerSessionCache = referrerSessionCache;
    }

    public void logout(final HttpSession session,
                       final HttpServletRequest request,
                       final HttpServletResponse response) {
        notNull(session, "HTTP session is required");
        notNull(request, "HTTP request is required");
        notNull(response, "HTTP response is required");

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession != null) {
            sessionService.invalidateByPlayerAndSessionKey(lobbySession.getPlayerId(), lobbySession.getLocalSessionKey());
        }

        final Cookie cookie = lobbySessionCache.invalidateLocalSession();
        session.invalidate();
        response.addCookie(cookie);

        referrerSessionCache.invalidate();
        rememberMeHandler.forgetMe(response);
        cookieHelper.invalidateCanvas(response);
    }
}
