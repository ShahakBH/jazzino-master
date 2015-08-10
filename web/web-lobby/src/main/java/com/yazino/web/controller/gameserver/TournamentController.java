package com.yazino.web.controller.gameserver;

import com.yazino.platform.Partner;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.tournament.*;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.data.TournamentDetailHelper;
import com.yazino.web.domain.LaunchConfiguration;
import com.yazino.web.domain.TournamentRequestType;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notNull;

@Controller("legacyTournamentController")
public class TournamentController {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentController.class);

    private static final String GAME_TYPE = "gameType";
    private static final String DEFAULT_GAME_TYPE = "BLACKJACK";

    private final LaunchConfiguration launchConfiguration;
    private final LobbySessionCache lobbySessionCache;
    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final WebApiResponses webApiResponses;

    @Autowired
    public TournamentController(final LaunchConfiguration launchConfiguration,
                                final LobbySessionCache lobbySessionCache,
                                final PlayerService playerService,
                                final TournamentService tournamentService,
                                final WebApiResponses webApiResponses) {
        notNull(launchConfiguration, "launchConfiguration may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(tournamentService, "tournamentService may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.launchConfiguration = launchConfiguration;
        this.lobbySessionCache = lobbySessionCache;
        this.playerService = playerService;
        this.tournamentService = tournamentService;
        this.webApiResponses = webApiResponses;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/tournament")
    public ModelAndView tournamentCommand(final HttpServletRequest request,
                                          final HttpServletResponse response) throws Exception {
        if (!request.getMethod().equals("POST")) {
            response.sendError(HttpStatus.OK.value(), "ERROR|Only POST accepted");
            return null;
        }
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpStatus.OK.value(), "ERROR|Session Expired");
            return null;
        }
        try {
            final BufferedReader reader = request.getReader();
            final String command = reader.readLine();
            LOG.debug("Tournament Command received {}", command);
            final String[] toSend = command.split("\\|");

            final String requestTypeAsString = toSend[0];
            notNull(requestTypeAsString, "Request Type may not be null");
            final TournamentRequestType requestType = TournamentRequestType.valueOf(requestTypeAsString);

            final String tournamentIdText = toSend[1];
            final BigDecimal tournamentId = new BigDecimal(tournamentIdText);
            final BigDecimal playerId = lobbySession.getPlayerId();

            LOG.debug("Sending Tournament request requestType=[{}] playerId=[{}] tournamentId=[{}]", requestType, playerId, tournamentId);

            switch (requestType) {
                case TOURNAMENT_PLAYER_REGISTER:
                    tournamentService.register(tournamentId, playerId, true);
                    break;

                case TOURNAMENT_PLAYER_UNREGISTER:
                    tournamentService.deregister(tournamentId, playerId, true);
                    break;

                default:
                    break;
            }

        } catch (Throwable e) {
            LOG.error("Request failed", e);
            response.sendError(HttpStatus.OK.value(), "ERROR|Command rejected");
            return null;
        }
        response.getWriter().write("OK|Command sent");
        return null;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/tournamentAction")
    public void register(@RequestParam("tournamentId") final int tournamentId,
                         @RequestParam("action") final String action,
                         final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpStatus.OK.value(), "ERROR|Session Expired");
            return;
        }
        final BigDecimal tournamentIdValue = BigDecimal.valueOf(tournamentId);
        final BigDecimal playerId = lobbySession.getPlayerId();
        final TournamentOperationResult result;
        if ("register".equalsIgnoreCase(action)) {
            result = tournamentService.register(tournamentIdValue, playerId, false);
        } else {
            result = tournamentService.deregister(tournamentIdValue, playerId, false);
        }
        response.setContentType("text/javascript");
        response.getWriter().write(String.valueOf(result));
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/nextTournament")
    public void getNextTournament(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  @RequestParam(value = GAME_TYPE, required = false) final String gameType)
            throws IOException {
        BigDecimal playerId = null;

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession != null) {
            playerId = lobbySession.getPlayerId();
        }

        final TournamentDetail tournamentDetail = nextTournamentFor(
                playerId, getGameType(gameType), launchConfiguration.getPartnerId());

        final Map<String, Object> result = new HashMap<>();
        if (tournamentDetail != null) {
            result.put("id", tournamentDetail.getTournamentId());
            result.put("name", tournamentDetail.getName());
            result.put("gameType", tournamentDetail.getGameType());
            result.put("description", tournamentDetail.getDescription());
            result.put("inProgress", tournamentDetail.getInProgress());
            result.put("millisToStart", defaultIfNull(tournamentDetail.getMillisToStart(), 0));
            result.put("registeredPlayers", defaultIfNull(tournamentDetail.getRegisteredPlayers(), 0));
            result.put("registeredFriends", defaultIfNull(tournamentDetail.getRegisteredFriends(), 0));
            result.put("currentPlayerRegistered", defaultIfNull(tournamentDetail.getPlayerRegistered(), false));
            result.put("prizePool", defaultIfNull(tournamentDetail.getPrizePool(), BigDecimal.ZERO));
            result.put("firstPrize", defaultIfNull(tournamentDetail.getFirstPrize(), BigDecimal.ZERO));
            result.put("registrationFee", defaultIfNull(tournamentDetail.getRegistrationFee(), BigDecimal.ZERO));
        }
        webApiResponses.writeOk(response, result);
    }

    private String getGameType(final String gameTypeParameter) {
        if (gameTypeParameter == null) {
            return DEFAULT_GAME_TYPE;
        }
        return gameTypeParameter;
    }

    private TournamentDetail nextTournamentFor(final BigDecimal playerId,
                                               final String gameType,
                                               final Partner partnerId) {
        notNull(gameType, "gameType is null");
        notNull(partnerId, "partnerId is null");

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Finding tournament with the nearest start time for "
                            + "game type = %s, partner Id = %s and player id=%s",
                    gameType, partnerId, playerId));
        }

        final Set<TournamentRegistrationInfo> tournaments = tournamentsFor(playerId, gameType, partnerId);
        if (tournaments.isEmpty()) {
            LOG.warn(String.format("No tournaments found for gameType [%s], partnerId [%s]", gameType, partnerId));
            return null;
        }

        final TournamentRegistrationInfo first = tournaments.iterator().next();
        return TournamentDetailHelper.buildTournamentDetail(
                gameType, playerId, friendsForPlayer(playerId), first);

    }

    private Set<TournamentRegistrationInfo> tournamentsFor(final BigDecimal playerId,
                                                           final String gameType,
                                                           final Partner partnerId) {
        final Schedule schedule;
        try {
            schedule = tournamentService.getTournamentSchedule(gameType);

        } catch (Exception e) {
            LOG.error(String.format("Couldn't fetch schedule for partner %s and gameType %s",
                    partnerId, gameType));
            return emptySet();
        }

        if (schedule == null) {
            LOG.warn("No tournament schedule is available for partner {} and gametype {}", partnerId, gameType);
            return emptySet();
        }

        if (playerId != null) {
            return schedule.getChronologicalTournamentsForPlayer(playerId);
        }
        return schedule.getChronologicalTournaments();
    }

    private Set<BigDecimal> friendsForPlayer(final BigDecimal playerId) {
        if (playerId != null) {
            return playerService.getFriends(playerId);
        } else {
            return emptySet();
        }
    }
}
