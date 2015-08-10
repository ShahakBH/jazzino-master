package com.yazino.web.controller;

import com.yazino.platform.session.InboxService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class InboxController {
    private final LobbySessionCache lobbySessionCache;
    private final InboxService inboxService;

    @Autowired
    public InboxController(final LobbySessionCache lobbySessionCache,
                           final InboxService inboxService) {
        notNull(inboxService, "inboxService is null");
        notNull(lobbySessionCache, "lobbySessionCache is null");

        this.lobbySessionCache = lobbySessionCache;
        this.inboxService = inboxService;
    }

    @RequestMapping({"/lobby/checkNewMessages", "/messages/check"})
    public void checkNewMessages(final HttpServletRequest request,
                                 final HttpServletResponse response)
            throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        inboxService.checkNewMessages(lobbySession.getPlayerId());

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
