package com.yazino.web.controller;


import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.tournament.*;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.domain.TrophyLeaderBoardSummary;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.PlayerFriendsCache;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class GameTournamentController {
    private static final Logger LOG = LoggerFactory.getLogger(GameTournamentController.class);

    public enum LeaderBoardType {
        WORLD,
        FRIENDS
    }

    private static final int MAX_TO_DISPLAY = 50;
    public static final String EMPTY_RESPONSE = "{}";

    private static final String GAME_TYPE = "gameType";
    private static final String DEFAULT_GAME_TYPE = "BLACKJACK";

    private final LobbySessionCache lobbySessionCache;
    private final TournamentService tournamentService;
    private final TournamentLobbyCache tournamentLobbyCache;
    private final PlayerTrophyService playerTrophyService;
    private final PlayerFriendsCache playerFriendsCache;

    private final WebApiResponses responseWriter;

    private String trophyName = "";

    @Autowired
    public GameTournamentController(final LobbySessionCache lobbySessionCache,
                                    final TournamentLobbyCache tournamentLobbyCache,
                                    final TournamentService tournamentService,
                                    final PlayerTrophyService playerTrophyService,
                                    @Qualifier("playerFriendsCache") final PlayerFriendsCache playerFriendsCache,
                                    final WebApiResponses responseWriter) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(tournamentLobbyCache, "tournamentLobbyCache is null");
        notNull(tournamentService, "tournamentService is null");
        notNull(playerTrophyService, "playerTrophyService is null");
        notNull(playerFriendsCache, "Player Friends Cache may not be null");

        this.tournamentLobbyCache = tournamentLobbyCache;
        this.lobbySessionCache = lobbySessionCache;
        this.tournamentService = tournamentService;
        this.playerTrophyService = playerTrophyService;
        this.playerFriendsCache = playerFriendsCache;
        this.responseWriter = responseWriter;
    }

    @RequestMapping({"/public/nextTournament",
            "/lobbyCommand/nextTournament",
            "/tournaments/next"})
    public void getNextTournament(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  @RequestParam(value = GAME_TYPE, required = false) final String gameType)
            throws IOException {
        BigDecimal playerId = null;

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession != null) {
            playerId = lobbySession.getPlayerId();
        }

        final TournamentDetail tournamentDetail = tournamentLobbyCache.getNextTournament(
                playerId, getGameType(gameType));
        if (tournamentDetail != null) {
            toJson(response, tournamentDetail);
        }
    }

    @RequestMapping({"/lobbyCommand/registerTournament", "/tournaments/register"})
    public void register(@RequestParam(value = "tournamentId", required = false) final Integer tournamentId,
                         final HttpServletRequest request,
                         final HttpServletResponse response)
            throws IOException {
        if (!hasParameter("tournamentId", tournamentId, request, response)) {
            return;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendForbiddenStatus(response);
            return;
        }

        TournamentOperationResult operationResult;
        try {
            operationResult = tournamentService.register(
                    BigDecimal.valueOf(tournamentId), lobbySession.getPlayerId(), false);
        } catch (Exception e) {
            LOG.error(String.format("Registration failed for tournament %s with player %s",
                    tournamentId, lobbySession.getPlayerId()), e);
            operationResult = TournamentOperationResult.UNKNOWN;
        }

        final Map<String, Object> result = new HashMap<>();
        result.put("result", operationResult.name());
        responseWriter.writeOk(response, result);
    }

    private void sendForbiddenStatus(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            // ignored
        }
    }

    @RequestMapping({"/lobbyCommand/unregisterTournament", "/tournaments/unregister"})
    public void unregister(@RequestParam(value = "tournamentId", required = false) final Integer tournamentId,
                           final HttpServletRequest request,
                           final HttpServletResponse response)
            throws IOException {
        if (!hasParameter("tournamentId", tournamentId, request, response)) {
            return;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendForbiddenStatus(response);
            return;
        }

        TournamentOperationResult operationResult;
        try {
            operationResult = tournamentService.deregister(
                    BigDecimal.valueOf(tournamentId), lobbySession.getPlayerId(), false);
        } catch (Exception e) {
            LOG.error(String.format("Deregistration failed for tournament %s with player %s",
                    tournamentId, lobbySession.getPlayerId()), e);
            operationResult = TournamentOperationResult.UNKNOWN;
        }

        final Map<String, Object> result = new HashMap<>();
        result.put("result", operationResult.name());
        responseWriter.writeOk(response, result);
    }

    @RequestMapping({"/lobbyCommand/tournamentSchedule", "/tournaments/schedule"})
    public void tournamentSchedule(@RequestParam(value = "gameType", required = false) final String gameType,
                                   final HttpServletRequest request,
                                   final HttpServletResponse response) throws IOException {
        if (!hasParameter("gameType", gameType, request, response)) {
            return;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            toJson(response, Collections.emptyList());
            return;
        }

        final List<TournamentDetail> schedule = tournamentLobbyCache.getTournamentSchedule(
                gameType, lobbySession.getPlayerId());
        toJson(response, schedule);
    }

    @RequestMapping({"/lobbyCommand/trophyLeaderboard", "/tournaments/leaderboard"})
    public void trophyLeaderboard(@RequestParam(value = "gameType", required = false) final String gameType,
                                  @RequestParam(value = "maxRows", defaultValue = "10") final int maxRows,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) throws IOException {
        if (!hasParameter("gameType", gameType, request, response)) {
            return;
        }

        final List<TrophyLeaderboardPlayer> leaderBoard = tournamentLobbyCache.getTrophyLeaderBoard(gameType, maxRows);
        toJson(response, leaderBoard);
    }

    @RequestMapping("/tournaments/trophyLeaderboardSummary")
    @ResponseBody
    public String trophyLeaderBoardSummary(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           @RequestParam(value = "gameType", required = false) final String gameType,
                                           @RequestParam(value = "maxPlayers",
                                                   defaultValue = "10") final int maxPlayers,
                                           @RequestParam(value = "type", required = false) final LeaderBoardType type) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            return EMPTY_RESPONSE;
        }

        if (gameType == null) {
            return EMPTY_RESPONSE;
        }

        TrophyLeaderBoardSummary trophyLeaderBoardSummary;
        if (type != null && LeaderBoardType.FRIENDS == type) {
            final Set<BigDecimal> friendIds = playerFriendsCache.getFriendIds(lobbySession.getPlayerId());
            trophyLeaderBoardSummary = tournamentLobbyCache.getTrophyLeaderBoardSummaryForFriends(
                    lobbySession.getPlayerId(), gameType, friendIds, maxPlayers);
        } else {
            trophyLeaderBoardSummary = tournamentLobbyCache.getTrophyLeaderBoardSummary(
                    lobbySession.getPlayerId(), gameType, maxPlayers);
        }
        return new JsonHelper().serialize(trophyLeaderBoardSummary);
    }

    @RequestMapping("/tournaments/lastTournament")
    public void lastTournament(@RequestParam(value = "gameType", required = true) final String gameType,
                               final HttpServletResponse response) throws IOException {
        final Summary summary = tournamentLobbyCache.getLastTournamentSummary(gameType);
        if (summary == null) {
            toJson(response, Collections.<Object>emptyList());
        } else {
            toJson(response, summary.getPlayers());
        }
    }


    @RequestMapping("/tournaments/lastTournamentFriends")
    public void lastTournamentFriends(@RequestParam(value = "gameType", required = true) final String gameType,
                                      final HttpServletResponse response,
                                      final HttpServletRequest request)
            throws IOException {

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            toJson(response, Collections.<Object>emptyList());
            return;
        }

        final Set<BigDecimal> friendIds = playerFriendsCache.getFriendIds(lobbySession.getPlayerId());
        friendIds.add(lobbySession.getPlayerId());

        final Summary summary = tournamentLobbyCache.getLastTournamentSummary(gameType);
        if (summary == null) {
            toJson(response, Collections.<Object>emptyList());
            return;
        }
        toJson(response, summary.getPlayersWithIds(friendIds));
    }

    @RequestMapping("/tournaments/hallOfFame")
    public void hallOfFame(@RequestParam(value = "gameType", required = true) final String gameType,
                           final HttpServletResponse response) throws IOException {
        final Map<String, List<TrophyWinner>> winners = findWinners(gameType);
        final List<TrophyWinner> hallOfFame;
        if (winners.containsKey(gameType)) {
            hallOfFame = winners.get(gameType);
        } else {
            hallOfFame = Collections.<TrophyWinner>emptyList();
        }
        toJson(response, hallOfFame);
    }

    private Map<String, List<TrophyWinner>> findWinners(final String gameType) {
        try {
            return playerTrophyService.findWinnersByTrophyName(trophyName, MAX_TO_DISPLAY, gameType);
        } catch (Exception e) {
            LOG.error("Winner retrieval failed", e);
            return Collections.emptyMap();
        }
    }

    @Value("${strata.server.lobby.trophy}")
    public void setTrophyName(final String trophyName) {
        this.trophyName = trophyName;
    }


    @RequestMapping({"/lobbyCommand/trophyLeaderboardPersonal", "/tournaments/leaderboard/personal"})
    public void trophyLeaderboardPersonal(@RequestParam(value = "gameType", required = false) final String gameType,
                                          final HttpServletRequest request,
                                          final HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendForbiddenStatus(response);
            return;
        }

        if (!hasParameter("gameType", gameType, request, response)) {
            return;
        }

        final TrophyLeaderboardPlayer leaderBoard = tournamentLobbyCache.getTrophyLeaderBoardPlayer(
                gameType, lobbySession.getPlayerId());
        toJson(response, leaderBoard);
    }

    private void toJson(final HttpServletResponse response,
                        final Object toSend) {
        try {
            responseWriter.writeOk(response, toSend);
        } catch (IOException e) {
            LOG.warn("Failed to writeOk JSON to response: {}", e.getMessage(), e);
        }
    }

    private String getGameType(final String gameTypeParameter) {
        if (gameTypeParameter == null) {
            return DEFAULT_GAME_TYPE;
        }
        return gameTypeParameter;
    }

}
