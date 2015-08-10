package com.yazino.platform.repository.session;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.session.PlayerSessionStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class InMemoryPlayerSessionRepository implements PlayerSessionRepository {
    @Override
    public Collection<PlayerSession> findAllByPlayer(final BigDecimal playerId) {
        return null;
    }

    @Override
    public void save(final PlayerSession session) {
    }

    @Override
    public boolean isOnline(final BigDecimal playerId) {
        return true;
    }

    @Override
    public void extendCurrentSession(final BigDecimal playerId,
                                     final String sSessionKey) {
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
        return null;
    }

    @Override
    public void removeAllByPlayer(final BigDecimal playerId) {
    }

    @Override
    public PlayerSession findByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {
        return null;
    }

    @Override
    public void removeByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {

    }

    @Override
    public Map<BigDecimal, PlayerSessionsSummary> findOnlinePlayerSessions(final Set<BigDecimal> bigDecimals) {
        return null;
    }

    @Override
    public int countPlayerSessions(final boolean onlyPlaying) {
        return 0;
    }

    @Override
    public void updateGlobalPlayerList(final BigDecimal playerId) {
    }

    @Override
    public Set<PlayerSessionStatus> findAllSessionStatuses() {
        return Collections.emptySet();
    }
}
