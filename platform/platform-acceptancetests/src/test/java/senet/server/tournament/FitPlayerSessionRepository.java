package senet.server.tournament;


import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.PlayerSessionStatus;

import java.math.BigDecimal;
import java.util.*;

public class FitPlayerSessionRepository implements PlayerSessionRepository {

    private final Set<BigDecimal> offlinePlayers = new HashSet<>();

    public Collection<PlayerSession> findAllByPlayer(BigDecimal playerId) {
        return null;
    }

    public void save(PlayerSession session) {
    }

    public boolean isOnline(BigDecimal playerId) {
        return !offlinePlayers.contains(playerId);
    }

    @Override
    public PagedData<PlayerSession> findAll(final int page) {
        return PagedData.empty();
    }

    public Set<BigDecimal> findOnlinePlayers(Set<BigDecimal> playerKeys) {
        return playerKeys;
    }

    public Map<BigDecimal, PlayerSessionsSummary> findOnlinePlayerSessions(Set<BigDecimal> bigDecimals) {
        return new HashMap<>();
    }

    @Override
    public int countPlayerSessions(final boolean onlyPlaying) {
        return 0;
    }

    public void removeSession(BigDecimal playerId) {
        offlinePlayers.add(playerId);
    }

    @Override
    public void removeAllByPlayer(BigDecimal playerId) {
        removeSession(playerId);
    }

    @Override
    public PlayerSession findByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {
        return null;
    }

    @Override
    public void extendCurrentSession(final BigDecimal playerId, final String sessionKey) {

    }

    @Override
    public PlayerSession lock(final BigDecimal playerId, final String sessionKey) {
        return null;
    }

    @Override
    public void removeByPlayerAndSessionKey(final BigDecimal playerId, final String sessionKey) {
        removeSession(playerId);
    }

    @Override
    public void updateGlobalPlayerList(final BigDecimal playerId) {
    }

    @Override
    public Set<PlayerSessionStatus> findAllSessionStatuses() {
        return null;
    }
}
