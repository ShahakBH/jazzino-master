package com.yazino.web.controller.util;

import com.yazino.web.api.RequestException;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang.Validate.notNull;

@Component
public class RequestValidationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RequestValidationHelper.class);
    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public RequestValidationHelper(LobbySessionCache lobbySessionCache) {
        notNull(lobbySessionCache);
        this.lobbySessionCache = lobbySessionCache;
    }

    public LobbySession verifySession(HttpServletRequest request) throws RequestException {
        LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.info("No active session");
            throw new RequestException(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
        }
        return session;
    }
}
