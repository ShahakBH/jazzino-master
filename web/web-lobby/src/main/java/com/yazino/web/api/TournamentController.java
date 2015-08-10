package com.yazino.web.api;

import com.yazino.platform.tournament.TournamentView;
import com.yazino.web.data.TournamentViewRepository;
import com.yazino.web.domain.TournamentDetailView;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.PlayerFriendsCache;
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

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller("apiTournamentController")
@RequestMapping("/api/1.0/tournament/*")
public class TournamentController {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentController.class);

    private final LobbySessionCache lobbySessionCache;
    private final TournamentLobbyCache tournamentLobbyCache;
    private final TournamentViewRepository tournamentViewRepository;
    private final PlayerFriendsCache playerFriendsCache;
    private final WebApiResponses webApiResponses;

    @Autowired
    public TournamentController(final LobbySessionCache lobbySessionCache,
                                final TournamentLobbyCache tournamentLobbyCache,
                                final TournamentViewRepository tournamentViewRepository,
                                final PlayerFriendsCache playerFriendsCache,
                                final WebApiResponses webApiResponses) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(tournamentLobbyCache, "tournamentLobbyCache may not be null");
        notNull(tournamentViewRepository, "tournamentViewRepository may not be null");
        notNull(playerFriendsCache, "playerFriendsCache may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.tournamentLobbyCache = tournamentLobbyCache;
        this.tournamentViewRepository = tournamentViewRepository;
        this.playerFriendsCache = playerFriendsCache;
        this.webApiResponses = webApiResponses;
    }

    @RequestMapping(value = "/next/by-variation/{gameType}", method = RequestMethod.GET)
    public void nextTournamentsForAllVariations(@PathVariable("gameType") final String gameType,
                                                final HttpServletRequest request,
                                                final HttpServletResponse response) throws IOException {
        notBlank(gameType, "gameType may not be null/blank");
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

            webApiResponses.writeOk(response, tournamentLobbyCache.getNextTournamentsForEachVariation(playerId, gameType));

        } catch (Exception e) {
            LOG.error("Failed to get next tournaments for game type {} and player {}", gameType, playerId, e);
            webApiResponses.writeError(response, SC_INTERNAL_SERVER_ERROR,
                    String.format("Failed to get next tournaments for game type %s and player %s", gameType, playerId));
        }
    }

    @RequestMapping(value = "/{tournamentId}", method = RequestMethod.GET)
    public void tournamentDetailFor(@PathVariable("tournamentId") final BigDecimal tournamentId,
                                    final HttpServletRequest request,
                                    final HttpServletResponse response) throws IOException {
        notNull(tournamentId, "tournamentId may not be null");
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

            final TournamentView tournamentView = tournamentViewRepository.getTournamentView(tournamentId);
            if (tournamentView != null) {
                webApiResponses.writeOk(response, new TournamentDetailView(tournamentView, playerId, playerFriendsCache.getFriendIds(playerId)));

            } else {
                webApiResponses.writeNoContent(response, HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            LOG.error("Failed to get tournament detail for {} with player {}", tournamentId, playerId, e);
            webApiResponses.writeError(response, SC_INTERNAL_SERVER_ERROR,
                    String.format("Failed to get tournament detail for %s with player %s", tournamentId, playerId));
        }
    }

}
