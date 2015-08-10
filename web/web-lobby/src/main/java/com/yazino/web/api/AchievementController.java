package com.yazino.web.api;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.community.TrophyCabinet;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller("apiAchievementController")
@RequestMapping("/api/1.0/achievement/*")
public class AchievementController {
    private static final Logger LOG = LoggerFactory.getLogger(AchievementController.class);

    private static final String PROPERTY_MEDALS = "strata.server.lobby.medals";
    private static final String PROPERTY_TROPHIES = "strata.server.lobby.trophy";

    private final PlayerTrophyService playerTrophyService;
    private final LobbySessionCache lobbySessionCache;
    private final WebApiResponses webApiResponses;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public AchievementController(final PlayerTrophyService playerTrophyService,
                                 final LobbySessionCache lobbySessionCache,
                                 final WebApiResponses webApiResponses,
                                 final YazinoConfiguration yazinoConfiguration) {
        notNull(playerTrophyService, "playerTrophyService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.playerTrophyService = playerTrophyService;
        this.webApiResponses = webApiResponses;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @RequestMapping(value = "/trophies/summary/{gameType}", method = RequestMethod.GET)
    public void trophyCabinetSummary(@PathVariable("gameType") final String gameType,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response) throws IOException {
        notBlank(gameType, "gameType may not be null");
        notNull(request, "request may not be null");
        notNull(response, "response may not be null");

        BigDecimal playerId = null;
        try {
            final LobbySession currentSession = lobbySessionCache.getActiveSession(request);
            if (currentSession == null) {
                webApiResponses.writeError(response, SC_UNAUTHORIZED, "No session");
                return;
            }
            playerId = currentSession.getPlayerId();

            webApiResponses.writeOk(response, summariseTrophiesFor(gameType, playerId));

        } catch (Exception e) {
            LOG.error("Failed to summarise trophy cabinet for game type {} and player {}", gameType, playerId, e);
            webApiResponses.writeError(response, SC_INTERNAL_SERVER_ERROR,
                    String.format("Failed to summarise trophy cabinet summary for game type %s and player %s", gameType, playerId));
        }
    }

    private Map<String, Object> summariseTrophiesFor(final String gameType, final BigDecimal playerId) {
        final Map<String, Object> trophySummary = new HashMap<>();

        final List<String> trophyNames = trophyNames();

        final TrophyCabinet trophyCabinet = playerTrophyCabinet(gameType, playerId, trophyNames);
        if (trophyCabinet != null) {
            for (String trophyName : trophyCabinet.getTrophySummaries().keySet()) {
                trophySummary.put(trophyName, trophyCabinet.getTrophySummaries().get(trophyName).getCount());
            }
        }
        for (String trophyName : trophyNames) {
            if (!trophySummary.containsKey(trophyName)) {
                trophySummary.put(trophyName, 0);
            }
        }
        return trophySummary;
    }

    private TrophyCabinet playerTrophyCabinet(final String gameType,
                                              final BigDecimal playerId,
                                              final List<String> trophyNames) {
        try {
            return playerTrophyService.findTrophyCabinetForPlayer(gameType, playerId, trophyNames);

        } catch (Exception e) {
            LOG.error("Couldn't find trophy cabinet for player {} with names {}", playerId, trophyNames, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> trophyNames() {
        final List<String> trophyNames = new ArrayList<>();
        trophyNames.addAll((List) yazinoConfiguration.getList(PROPERTY_MEDALS, Collections.emptyList()));
        trophyNames.addAll((List) yazinoConfiguration.getList(PROPERTY_TROPHIES, Collections.emptyList()));
        return trophyNames;
    }
}
