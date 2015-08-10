package com.yazino.web.controller.gameserver;

import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.playerstatistic.service.PlayerStatsService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.LobbyInformation;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class CommunityController {
    private static final Logger LOG = LoggerFactory.getLogger(CommunityController.class);

    private final LobbyInformationService lobbyInformationService;
    private final LobbySessionCache lobbySessionCache;
    private final PlayerStatsService playerStatsService;
    private final CommunityService communityService;
    private final WebApiResponses webApiResponses;

    @Autowired
    public CommunityController(final LobbyInformationService lobbyInformationService,
                               final LobbySessionCache lobbySessionCache,
                               final PlayerStatsService playerStatsService,
                               final CommunityService communityService,
                               final WebApiResponses webApiResponses) {
        notNull(lobbyInformationService, "lobbyInformationService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(playerStatsService, "playerStatsService may not be null");
        notNull(communityService, "communityService may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.lobbyInformationService = lobbyInformationService;
        this.lobbySessionCache = lobbySessionCache;
        this.playerStatsService = playerStatsService;
        this.communityService = communityService;
        this.webApiResponses = webApiResponses;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/community")
    public ModelAndView handleCommunity(final HttpServletRequest request,
                                        final HttpServletResponse response) throws Exception {
        final BufferedReader reader = request.getReader();
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpServletResponse.SC_OK, "Session Expired");
            return null;
        }
        try {
            final String command = reader.readLine();
            if (isBlank(command)) {
                response.sendError(HttpServletResponse.SC_OK, "No command submitted");
                return null;
            }

            LOG.debug("Received command [{},{}] {}", lobbySession.getPlayerId(), lobbySession.getPlayerName(), command);

            if (command.startsWith("publish")) {
                final String[] tokens = command.split("\\|");
                String gameType = null;
                if (tokens.length == 2) {
                    gameType = tokens[1];
                }
                communityService.asyncPublishCommunityStatus(lobbySession.getPlayerId(), gameType);
                playerStatsService.publishExperience(lobbySession.getPlayerId(), gameType);

            } else if (command.startsWith("request|")) {
                final String[] args = command.split("\\|");
                communityService.asyncRequestRelationshipChange(lobbySession.getPlayerId(),
                        new BigDecimal(args[1]), RelationshipAction.valueOf(args[2]));
            }

        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_OK, e.getMessage());
            return null;
        }
        response.setContentType("text/html");
        response.getWriter().write("OK");
        return null;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/lobbyInformation")
    public void lobbyInformation(final HttpServletResponse response,
                                 @RequestParam(value = "gameType", required = false) final String gameType)
            throws IOException {
        final LobbyInformation lobbyInformation = lobbyInformationService.getLobbyInformation(gameType);

        final Map<String, Object> model = new HashMap<>();
        model.put("onlinePlayers", Integer.toString(lobbyInformation.getOnlinePlayers()));
        model.put("activeTables", Integer.toString(lobbyInformation.getActiveTables()));
        webApiResponses.writeOk(response, model);
    }

}
