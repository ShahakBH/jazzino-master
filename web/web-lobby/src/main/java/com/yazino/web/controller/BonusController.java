package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.bonus.BonusException;
import com.yazino.platform.bonus.BonusService;
import com.yazino.platform.bonus.BonusStatus;
import com.yazino.platform.community.CommunityService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class BonusController {

    private final WebApiResponses responseWriter;
    private static final Logger LOG = LoggerFactory.getLogger(BonusController.class);
    private final CommunityService communityService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public BonusController(final WebApiResponses responseWriter,
                           final CommunityService communityService,
                           final YazinoConfiguration yazinoConfiguration,
                           final BonusService bonusService,
                           @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache
    ) {
        notNull(lobbySessionCache, "lobbySessionCache cannot be null");
        notNull(responseWriter, "responseWriter cannot be null");
        notNull(bonusService, "bonusService cannot be null");
        notNull(communityService, "communityService cannot be null");
        notNull(yazinoConfiguration);

        this.yazinoConfiguration = yazinoConfiguration;
        this.communityService = communityService;
        this.lobbySessionCache = lobbySessionCache;
        this.bonusService = bonusService;
        this.responseWriter = responseWriter;

    }

    final private BonusService bonusService;

    final private LobbySessionCache lobbySessionCache;

    //returns current BonusStatus
    @RequestMapping(value = "/api/1.0/bonus/status", method = RequestMethod.GET)
    public void getBonusStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (!validateLobbySession(session, response)) {
            return;
        }
        LOG.info("getting bonus status for {}", session.getPlayerId());
        //need to default this to unavailable if there's platform issues
        final BonusStatus bonusStatus;
        try {
            bonusStatus = bonusService.getBonusStatus(session.getPlayerId());
            responseWriter.writeOk(response, bonusStatus);
        } catch (Exception e) {
            LOG.error("bonus Service unavailable", e);
            responseWriter.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "bonus Service not available");
        }
    }

    //returns new bonusStatus on success, otherwise returns error message
    @RequestMapping(value = "/api/1.0/bonus/collect", method = RequestMethod.GET)
    public void collectBonus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        final boolean lockoutDisabled = yazinoConfiguration.getBoolean("strata.server.lobby.lockout.bonus.disabled", false);
        if (!validateLobbySession(session, response)) {
            return;
        }
        if(lockoutDisabled){
            responseWriter.writeError(response, HttpServletResponse.SC_FORBIDDEN, "Bonus collection disabled");
            return;
        }
        LOG.info("collecting bonus for {}", session.getPlayerId());

        try {
            final BonusStatus nextBonus = bonusService.collectBonus(session.getPlayerId(), session.getSessionId());
            communityService.asyncPublishBalance(session.getPlayerId());

            responseWriter.writeOk(response, nextBonus);
        } catch (BonusException e) {
            responseWriter.writeError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            LOG.error("bonus Service unavailable", e);
            responseWriter.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "bonus Service not available");
        }

    }

    public boolean validateLobbySession(LobbySession activeSession, HttpServletResponse response) throws IOException {
        if (activeSession == null) {
            LOG.warn("no active lobby session");
            responseWriter.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "no active session");
            return false;
        }
        if (activeSession.getPlayerId() == null) {
            LOG.warn("no playerId in lobby session");
            responseWriter.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "no playerId in lobby session");
            return false;
        }
        return true;
    }
}
