package com.yazino.web.api;

import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static javax.servlet.http.HttpServletResponse.*;

@Controller
public class ExternalFriendController {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalFriendController.class);

    private final AuthenticationService authenticationService;
    private final LobbySessionCache lobbySessionCache;
    private final WebApiResponses webApiResponses;


    @Autowired
    public ExternalFriendController(AuthenticationService authenticationService,
                                    LobbySessionCache lobbySessionCache,
                                    WebApiResponses webApiResponses) {
        this.authenticationService = authenticationService;
        this.lobbySessionCache = lobbySessionCache;
        this.webApiResponses = webApiResponses;
    }

    @RequestMapping(value = "/api/1.0/social/syncBuddies/{providerName}", method = RequestMethod.POST)
    public void registerExternalFriends(HttpServletRequest request,
                                        HttpServletResponse response,
                                        @PathVariable String providerName,
                                        @RequestParam String externalIds) throws IOException {
        LOG.debug("Sync buddies for {}: {}", providerName, externalIds);
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            LOG.debug("No session. Ignoring...");
            webApiResponses.writeError(response, SC_UNAUTHORIZED, "no session");
            return;
        }

        final Set<String> ids = extractExternalIds(externalIds);

        if (ids.isEmpty()) {
            LOG.debug("External ids is empty. Ignoring...");
            webApiResponses.writeError(response, SC_BAD_REQUEST, "empty externalIds");
            return;
        }

        LOG.debug("Sync {} buddies for player {}: {}", providerName, lobbySession.getPlayerId(), ids);

        authenticationService.syncBuddies(lobbySession.getPlayerId(), providerName, ids);
        webApiResponses.writeNoContent(response, SC_OK);
    }

    private Set<String> extractExternalIds(String externalIds) {
        if (StringUtils.isBlank(externalIds)) {
            return Collections.emptySet();
        }
        final Set<String> externalIdSet = new HashSet<>();
        final String[] tokens = externalIds.split(",");
        for (String token : tokens) {
            final String trimmedString = token.trim();
            if (!StringUtils.isBlank(trimmedString)) {
                externalIdSet.add(trimmedString);
            }
        }
        return externalIdSet;
    }

}
