package com.yazino.platform.model.session;

import com.yazino.game.api.time.SystemTimeSource;
import com.yazino.game.api.time.TimeSource;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.InvalidPlayerSessionException;
import com.yazino.platform.session.Location;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.apache.commons.lang3.Validate.notBlank;

public class GlobalPlayers implements Serializable {
    private static final long serialVersionUID = -7848516357252510550L;
    private static final Logger LOG = LoggerFactory.getLogger(GlobalPlayers.class);

    private final Set<GlobalPlayer> players = new ConcurrentSkipListSet<>();
    private final String gameType;
    private final int maximumPlayers;
    private final TimeSource timeSource;
    private final long timeout;
    private long lastScavenged = 0;

    public GlobalPlayers(final String gameType,
                         final int maximumPlayers,
                         final long timeout) {
        notBlank(gameType, "gameType is null");
        this.gameType = gameType;
        this.maximumPlayers = maximumPlayers;
        this.timeSource = new SystemTimeSource();
        this.timeout = timeout;
    }

    public boolean addPlayer(final Collection<PlayerSession> sessions,
                             final PlayerSessionRepository playerSessionRepository) {
        if (sessions.isEmpty()) {
            LOG.warn("Global {} players - Couldn't add player with no sessions", gameType);
            return false;
        }

        refreshList(playerSessionRepository);

        final PlayerSession firstSession = sessions.iterator().next();
        final GlobalPlayer player;
        try {
            player = new GlobalPlayer(gameType,
                    firstSession.getPlayerId(),
                    firstSession.getNickname(),
                    firstSession.getPictureUrl(),
                    firstSession.getBalanceSnapshot(),
                    locationsFrom(sessions));
        } catch (InvalidPlayerSessionException e) {
            LOG.warn("Global {} players - Couldn't add player sessions {}", gameType, sessions, e);
            return false;
        }
        final boolean hasPublicLocations = hasPublicLocations(sessions);
        final boolean result = players.size() < maximumPlayers
                && hasPublicLocations
                && players.add(player);
        LOG.debug("Global {} players - Added player {} ({}) ? {} (hasPublicLocationsForGameType? {}, canFitNewPlayer? {})",
                gameType, player.getNickname(), player.getPlayerId(), result, hasPublicLocations, players.size() < maximumPlayers);
        return result;
    }

    private Set<Location> locationsFrom(final Collection<PlayerSession> sessions) {
        final Set<Location> locations = new HashSet<>();
        for (PlayerSession session : sessions) {
            locations.addAll(session.getLocations());
        }
        return locations;
    }

    private boolean hasPublicLocations(final Collection<PlayerSession> sessions) {
        for (PlayerSession session : sessions) {
            if (session.hasPublicLocations(gameType)) {
                return true;
            }
        }
        return false;
    }

    private void refreshList(final PlayerSessionRepository playerSessionRepository) {
        final long now = timeSource.getCurrentTimeStamp();
        if ((now - timeout) <= lastScavenged) {
            LOG.debug("Global {} players - Not time to refresh yet", gameType);
            return;
        }

        LOG.debug("Global {} players - Time to refresh list", gameType);

        final Iterator<GlobalPlayer> playerIterator = players.iterator();
        while (playerIterator.hasNext()) {
            final GlobalPlayer player = playerIterator.next();
            final Collection<PlayerSession> playerSessions = playerSessionRepository.findAllByPlayer(player.getPlayerId());
            if (playerSessions.isEmpty() || playerHasNoPublicLocationsForGameType(playerSessions)) {
                LOG.debug("Global {} players - Player {} is now invalid. Removing...", gameType, player.getPlayerId());
                playerIterator.remove();
            }
        }
        lastScavenged = now;
    }

    private boolean playerHasNoPublicLocationsForGameType(final Collection<PlayerSession> playerSessions) {
        for (PlayerSession session : playerSessions) {
            if (session.hasPublicLocations(gameType)) {
                return false;
            }
        }
        return true;
    }

    public Set<GlobalPlayer> getCurrentList() {
        final HashSet<GlobalPlayer> result = new HashSet<>();
        for (GlobalPlayer player : players) {
            result.add(player);
        }
        return result;
    }

    public boolean contains(final BigDecimal playerId) {
        for (GlobalPlayer player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(final BigDecimal playerId) {
        final Iterator<GlobalPlayer> playerIterator = players.iterator();
        while (playerIterator.hasNext()) {
            if (playerIterator.next().getPlayerId().equals(playerId)) {
                LOG.debug("Global {} players - Removing player {}...", gameType, playerId);
                playerIterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
