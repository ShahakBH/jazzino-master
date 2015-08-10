package com.yazino.host.session;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.PlayerSessionStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;

@Repository
public class StandalonePlayerSessionRepository implements PlayerSessionRepository {
    private final Map<BigDecimal, PlayerSession> sessions = new HashMap<>();

    @Override
    public Collection<PlayerSession> findAllByPlayer(final BigDecimal playerId) {
        return asList(sessions.get(playerId));
    }

    @Override
    public void save(final PlayerSession session) {
        sessions.put(session.getPlayerId(), session);
    }

    @Override
    public boolean isOnline(final BigDecimal playerId) {
        return true;
    }

    @Override
    public PagedData<PlayerSession> findAll(final int page) {
        return null;
    }

    @Override
    public Set<BigDecimal> findOnlinePlayers(final Set<BigDecimal> playerKeys) {
        return null;
    }

    @Override
    public PlayerSession lock(final BigDecimal playerId,
                              final String sessionKey) {
        return sessions.get(playerId);
    }

    @Override
    public void removeAllByPlayer(final BigDecimal playerId) {
        sessions.remove(playerId);
    }

    @Override
    public PlayerSession findByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {
        return sessions.get(playerId);
    }

    @Override
    public void extendCurrentSession(final BigDecimal playerId, final String sessionKey) {

    }

    @Override
    public void removeByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {
        sessions.remove(playerId);
    }

    @Override
    public Map<BigDecimal, PlayerSessionsSummary> findOnlinePlayerSessions(final Set<BigDecimal> bigDecimals) {
        return null;
    }

    @Override
    public int countPlayerSessions(final boolean onlyPlaying) {
        return sessions.size();
    }

    @Override
    public void updateGlobalPlayerList(final BigDecimal playerId) {
    }

    @Override
    public Set<PlayerSessionStatus> findAllSessionStatuses() {
        final Set<PlayerSessionStatus> playerSessions = new HashSet<>();
        for (PlayerSession playerSession : sessions.values()) {
            playerSessions.add(new PlayerSessionStatus(playerSession.retrieveLocationIds(), playerSession.getPlayerId()));
        }
        return playerSessions;
    }
}
