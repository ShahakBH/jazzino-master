package com.yazino.web.service;

import com.yazino.platform.session.SessionService;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.web.domain.LobbyInformation;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

@Service("lobbyInformationService")
public class CachingLobbyInformationService implements LobbyInformationService {
    private static final Logger LOG = LoggerFactory.getLogger(CachingLobbyInformationService.class);

    private final SessionService sessionService;
    private final TableService tableService;

    private ConcurrentHashMap<String, CachedLobbyInformation> cache;

    private static final long LEASE_PERIOD = 30000;

    @Autowired
    public CachingLobbyInformationService(final SessionService sessionService,
                                          final TableService tableService) {
        notNull(sessionService, "sessionService may not be null");
        notNull(tableService, "tableService may not be null");

        this.sessionService = sessionService;
        this.tableService = tableService;

        cache = new ConcurrentHashMap<String, CachedLobbyInformation>();
    }

    @Override
    public LobbyInformation getLobbyInformation(final String gameType) {
        final LobbyInformation returnValue;
        final String key = keyFor(gameType);

        if (cache.containsKey(key)) {
            final CachedLobbyInformation cachedLobbyInformation = cache.get(key);
            if (cachedObjectAge(cachedLobbyInformation) > LEASE_PERIOD) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Lobby Information for key " + gameType + " has expired - refetching");
                }
                returnValue = fetchAndCache(gameType, key);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Lobby Information for key " + gameType + " found in cache");
                }
                returnValue = cachedLobbyInformation.getLobbyInformation();
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No lobbyInformation cache for" + gameType + " fetching and putting in cache");
            }
            returnValue = fetchAndCache(gameType, key);
        }
        return returnValue;
    }

    private String keyFor(final String gameType) {
        final String key;
        if (gameType == null) {
            key = "null";
        } else {
            key = gameType;
        }
        return key;
    }

    private LobbyInformation fetchAndCache(final String gameType,
                                           final String key) {
        final LobbyInformation returnValue = forGameType(gameType);
        cache.put(key, new CachedLobbyInformation(returnValue));
        return returnValue;
    }

    private long cachedObjectAge(final CachedLobbyInformation cachedLobbyInformation) {
        return DateTimeUtils.currentTimeMillis() - cachedLobbyInformation.getCreation();
    }

    private LobbyInformation forGameType(final String gameType) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving lobby information for game type " + gameType);
        }

        final int onlinePlayers = sessionService.countSessions(false);
        final int activeTables = tableService.countTablesWithPlayers(gameType);

        final LobbyInformation result =
                new LobbyInformation(gameType, onlinePlayers, activeTables, availabilityFor(gameType));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Current lobby status: " + ReflectionToStringBuilder.reflectionToString(result));
        }
        return result;
    }

    private boolean availabilityFor(final String gameType) {
        boolean gameAvailable = false;
        final Set<GameTypeInformation> gameTypes = tableService.getGameTypes();
        LOG.debug("Fetched game types from table service: {}", gameTypes);
        for (final GameTypeInformation type : gameTypes) {
            if (type.getId().equals(gameType)) {
                gameAvailable = type.isAvailable();
                break;
            }
        }
        return gameAvailable;
    }

    public class CachedLobbyInformation {
        private LobbyInformation lobbyInformation;
        private long creation;

        public CachedLobbyInformation(final LobbyInformation lobbyInformation) {
            this.lobbyInformation = lobbyInformation;
            this.creation = DateTimeUtils.currentTimeMillis();
        }

        public LobbyInformation getLobbyInformation() {
            return lobbyInformation;
        }

        public long getCreation() {
            return creation;
        }
    }
}


