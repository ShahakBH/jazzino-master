package com.yazino.platform.model.session;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.PlayerLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpaceClass
public class GlobalPlayerList implements Serializable {
    private static final long serialVersionUID = -8063798379687319438L;

    private static final Logger LOG = LoggerFactory.getLogger(GlobalPlayerList.class);

    public static final Integer DEFAULT_ID = 1;
    private static final long SCAVENGE_TIMEOUT = 60000;
    private static final int DEFAULT_GLOBAL_LIST_SIZE = 18;

    private Map<String, GlobalPlayers> globalList;
    private int globalListSize = DEFAULT_GLOBAL_LIST_SIZE;

    @SpaceId
    @SpaceRouting
    public Integer getId() {
        return DEFAULT_ID;
    }

    @SuppressWarnings("UnusedParameters")
    public void setId(final Integer id) {
        // read-only
    }

    @SuppressWarnings("UnusedDeclaration")
    public Map<String, GlobalPlayers> getGlobalList() {
        return globalList;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setGlobalList(final Map<String, GlobalPlayers> globalList) {
        this.globalList = globalList;
    }

    void setGlobalListSize(final int globalListSize) {
        this.globalListSize = globalListSize;
    }

    int getGlobalListSize() {
        return globalListSize;
    }

    private Map<String, GlobalPlayers> globalList() {
        if (globalList == null) {
            globalList = new ConcurrentHashMap<>();
        }
        return globalList;
    }

    public GlobalPlayers retrievePlayerList(final String gameType) {
        GlobalPlayers players = globalList().get(gameType);
        if (players == null) {
            players = new GlobalPlayers(gameType, globalListSize, SCAVENGE_TIMEOUT);
            globalList.put(gameType, players);
        }
        return players;
    }

    public Set<PlayerLocations> currentLocations() {
        final Collection<GlobalPlayer> globalPlayers = currentGlobalList();
        final Set<PlayerLocations> result = new HashSet<>();
        for (GlobalPlayer globalPlayer : globalPlayers) {
            result.add(globalPlayer.toLocations());
        }
        return result;
    }

    public boolean playerLocationChanged(final BigDecimal playerId,
                                         final Collection<PlayerSession> playerSession,
                                         final PlayerSessionRepository playerSessionRepository) {
        final Set<String> gameTypesForPlayer = gameTypesFor(playerSession);
        if (gameTypesForPlayer.isEmpty()) {
            LOG.debug("Player {} - No locations in session , trying to remove player", playerId);
            return tryToRemove(playerId);
        }
        LOG.debug("Player {} - {} games, trying to add to global player list.", playerId, gameTypesForPlayer.size());
        return updatePlayerLocations(playerId, playerSession, playerSessionRepository, gameTypesForPlayer);
    }

    private Set<String> gameTypesFor(final Collection<PlayerSession> playerSession) {
        final Set<String> gameTypes = new HashSet<>();
        for (PlayerSession session : playerSession) {
            gameTypes.addAll(session.retrieveGameTypesFromPublicLocations());
        }
        return gameTypes;
    }

    private boolean updatePlayerLocations(final BigDecimal playerId,
                                          final Collection<PlayerSession> playerSessions,
                                          final PlayerSessionRepository playerSessionRepository,
                                          final Set<String> gameTypesForPlayer) {
        boolean listChanged = false;
        final Set<String> gameTypes = getAllUsedGameTypes(gameTypesForPlayer);
        for (String gameType : gameTypes) {
            listChanged = listChanged || updatePlayerLocationsForGameType(playerId, playerSessions,
                    playerSessionRepository, gameTypesForPlayer, gameType);
        }
        return listChanged;
    }

    private boolean updatePlayerLocationsForGameType(final BigDecimal playerId,
                                                     final Collection<PlayerSession> playerSessions,
                                                     final PlayerSessionRepository playerSessionRepository,
                                                     final Set<String> gameTypesForPlayer,
                                                     final String gameType) {
        final GlobalPlayers players = retrievePlayerList(gameType);
        if (!gameTypesForPlayer.contains(gameType)) {
            return players.remove(playerId);
        }
        return players.addPlayer(playerSessions, playerSessionRepository);
    }

    private Set<String> getAllUsedGameTypes(final Set<String> playerGameTypes) {
        final Set<String> gameTypesInList = new HashSet<>();
        gameTypesInList.addAll(globalList().keySet());
        gameTypesInList.addAll(playerGameTypes);
        return gameTypesInList;
    }

    public boolean playerGoesOffline(final BigDecimal playerId) {
        return tryToRemove(playerId);
    }

    private boolean tryToRemove(final BigDecimal playerId) {
        boolean listChanged = false;
        for (GlobalPlayers globalPlayers : globalList().values()) {
            listChanged = listChanged | globalPlayers.remove(playerId);
        }
        return listChanged;
    }

    private Collection<GlobalPlayer> currentGlobalList() {
        final ArrayList<GlobalPlayer> result = new ArrayList<>();
        for (GlobalPlayers players : globalList().values()) {
            for (GlobalPlayer player : players.getCurrentList()) {
                result.add(player);
            }
        }
        return result;
    }

}
