package com.yazino.web.session;

import com.yazino.platform.session.SessionService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

public class LobbySessionCache implements Serializable {
    private static final long serialVersionUID = -5062874542812076713L;

    private static final Logger LOG = LoggerFactory.getLogger(LobbySessionCache.class);

    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = 60;
    public static final String COOKIE_KEY = "AFFILIATEID";
    static final String HEADER_KEY = "Y-Session";

    private final SessionService sessionService;
    private final ReadWriteLock lobbySessionLock = new ReentrantReadWriteLock();

    private int sessionCacheExpiryInSeconds = ONE_MINUTE;
    private long lastSessionCacheRenewal = -1;
    private LobbySession lobbySession;

    LobbySessionCache() {
        // for CGLib
        this.sessionService = null;
    }

    @Autowired
    public LobbySessionCache(final SessionService sessionService) {
        notNull(sessionService, "sessionService may not be null");

        this.sessionService = sessionService;
    }

    public void setLastSessionCacheRenewal(final long lastSessionCacheRenewal) {
        this.lastSessionCacheRenewal = lastSessionCacheRenewal;
    }

    public int getSessionCacheExpiryInSeconds() {
        return sessionCacheExpiryInSeconds;
    }

    public void setLobbySession(final LobbySession lobbySession) {
        LOG.debug("Setting lobby session to {}", lobbySession);
        verifyImplementationInstance();

        lobbySessionLock.writeLock().lock();
        try {
            this.lobbySession = lobbySession;
            this.lastSessionCacheRenewal = System.currentTimeMillis();
        } finally {
            lobbySessionLock.writeLock().unlock();
        }
    }

    private void verifyImplementationInstance() {
        if (sessionService == null) {
            throw new IllegalStateException("CGLib instance; business logic cannot be used on this instance");
        }
    }

    /**
     * retrieves an active lobby session based on
     * a) http session if a lobby session is there
     * b) http cookies if a lobby session is not there; but exists in the account detail service
     * c) http request header if lobby session and cookie not present, but exists in the account detail service
     * d) returns null if neither has a valid session
     * <p/>
     * in any case, this method will cache the lobby session in the session cache and then reuse it next time. it
     * will also validate and renew the session lease in the account detail service
     *
     * @param request the HTTP request.
     * @return the session, or null if none.
     */
    public LobbySession getActiveSession(final HttpServletRequest request) {
        LOG.debug("entering getActiveSession ");
        verifyImplementationInstance();

        lobbySessionLock.readLock().lock();
        try {
            if (isLocalSessionActive(request)) {
                if (localSessionUpForRenewal()) {
                    return renewSession();
                } else {
                    LOG.debug("local session active [{},{}] not for renewal, returning local",
                            lobbySession.getPlayerId(), lobbySession.getPlayerName());
                    return lobbySession;
                }
            } else {
                return resolveSessionIfAvailable(request);
            }
        } finally {
            lobbySessionLock.readLock().unlock();
        }
    }

    private LobbySession resolveSessionIfAvailable(final HttpServletRequest request) {
        LOG.debug("local session inactive, retrieving from cookies ");
        final LobbySessionReference sessionReference = getSessionRequestFromHeaderOrCookie(request);
        if (sessionReference == null) {
            return null;
        }

        final LobbySession validated = LobbySession.forSession(sessionService.authenticateAndExtendSession(
                sessionReference.getPlayerId(),
                sessionReference.getSessionKey()), true, sessionReference.getPlatform(), sessionReference.getAuthProvider());
        return updateSessionFrom(validated);
    }

    private LobbySession updateSessionFrom(final LobbySession validated) {
        LOG.debug("validated={}", validated);
        lobbySessionLock.readLock().unlock();
        try {
            if (validated == null) {
                invalidateLocalSession();
                return null;
            }
            setLobbySession(validated);

            LOG.debug("Session = {}", lobbySession);

            return lobbySession;

        } finally {
            lobbySessionLock.readLock().lock();
        }
    }

    private LobbySession renewSession() {
        LOG.debug("local session active [{},{}], up for renewal, validating against account detail svc",
                lobbySession.getPlayerId(), lobbySession.getPlayerName());
        final LobbySession validated = LobbySession.forSession(sessionService.authenticateAndExtendSession(
                lobbySession.getPlayerId(), lobbySession.getLocalSessionKey()), false,
                lobbySession.getPlatform(), lobbySession.getAuthProvider());
        return updateSessionFrom(validated);
    }

    /**
     * Invalidates the user's session by returning an invalidated session cookie.
     *
     * @return A new, invalidated session cookie.
     */
    public Cookie invalidateLocalSession() {
        verifyImplementationInstance();

        lobbySessionLock.writeLock().lock();
        try {
            lobbySession = null;
            lastSessionCacheRenewal = -1;
            final String val = "0";
            final Cookie cookie = createSessionCookie(val);
            cookie.setMaxAge(0);
            return cookie;
        } finally {
            lobbySessionLock.writeLock().unlock();
        }
    }

    private Cookie createSessionCookie(final String val) {
        final Cookie cookie = new Cookie(LobbySessionCache.COOKIE_KEY, val);
        cookie.setPath("/");
        return cookie;
    }

    private boolean localSessionUpForRenewal() {
        return lastSessionCacheRenewal < 0
                || (System.currentTimeMillis() - lastSessionCacheRenewal > ONE_SECOND * sessionCacheExpiryInSeconds);
    }

    private boolean isLocalSessionActive(final HttpServletRequest request) {
        if (lobbySession == null) {
            return false;
        }
        if (lobbySession.getLocalSessionKey() == null) {
            return false;
        }
        if (request != null && request.getAttribute("sessionRecreatedViaRememberMe") == null && hasSessionRequestHeaderOrCookie(request)) {
            final LobbySessionReference sessionReference = getSessionRequestFromHeaderOrCookie(request);
            if (sessionReference == null
                    || !ObjectUtils.equals(lobbySession.getLocalSessionKey(), sessionReference.getSessionKey())) {
                LOG.debug("Invalid local session ");
                return false;
            }
        }
        LOG.debug("Local session is active");
        return true;
    }

    public boolean hasSessionRequestHeaderOrCookie(final HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        final Cookie[] cookies = request.getCookies();
        boolean requestPresent = false;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_KEY.equals(cookie.getName()) && !StringUtils.isBlank(cookie.getValue())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cookie {} is present: {}", COOKIE_KEY, cookie.getValue());
                    }
                    requestPresent = true;
                }
            }
        }

        if (!requestPresent && !StringUtils.isBlank(request.getHeader(HEADER_KEY))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Header {} is present: {}", HEADER_KEY, request.getHeader(HEADER_KEY));
            }
            requestPresent = true;
        }

        return requestPresent;
    }

    private LobbySessionReference getSessionRequestFromHeaderOrCookie(final HttpServletRequest request) {
        LOG.debug("Getting session request from HTTP request.");
        if (request == null) {
            LOG.debug("HTTP request not present. Returning...");
            return null;
        }

        LobbySessionReference sessionReference = null;
        final Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_KEY.equals(cookie.getName())) {
                    final String encodedSession = cookie.getValue();
                    LOG.debug("Building session request based on cookie value");
                    sessionReference = LobbySessionReference.fromEncodedSession(encodedSession);
                }
            }
        }

        if (sessionReference == null && !StringUtils.isBlank(request.getHeader(HEADER_KEY))) {
            LOG.debug("Building session request based on header value");
            sessionReference = LobbySessionReference.fromEncodedSession(request.getHeader(HEADER_KEY));
        }

        return sessionReference;
    }

    private String getSessionEncodedValue() {
        if (lobbySession == null) {
            return null;
        }
        return new LobbySessionReference(lobbySession).encode();
    }

    Cookie generateLocalSessionCookie(final HttpServletRequest request) {
        lobbySessionLock.readLock().lock();
        try {
            if (!isLocalSessionActive(request)) {
                return null;
            }
            return createSessionCookie(getSessionEncodedValue());

        } finally {
            lobbySessionLock.readLock().unlock();
        }
    }

}
