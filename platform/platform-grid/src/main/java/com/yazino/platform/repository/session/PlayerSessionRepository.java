package com.yazino.platform.repository.session;


import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.session.PlayerSessionStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PlayerSessionRepository {
    Collection<PlayerSession> findAllByPlayer(BigDecimal playerId);

    PlayerSession findByPlayerAndSessionKey(BigDecimal playerId,
                                            String sessionKey);

    void save(PlayerSession session);

    boolean isOnline(BigDecimal playerId);

    void extendCurrentSession(BigDecimal playerId,
                              String sessionKey);

    PagedData<PlayerSession> findAll(int page);

    Set<BigDecimal> findOnlinePlayers(Set<BigDecimal> playerKeys);

    PlayerSession lock(BigDecimal playerId,
                       String sessionKey);

    void removeAllByPlayer(BigDecimal playerId);

    void removeByPlayerAndSessionKey(BigDecimal playerId,
                                     String sessionKey);

    Map<BigDecimal, PlayerSessionsSummary> findOnlinePlayerSessions(Set<BigDecimal> bigDecimals);

    int countPlayerSessions(boolean onlyPlaying);

    void updateGlobalPlayerList(BigDecimal playerId);

    Set<PlayerSessionStatus> findAllSessionStatuses();
}
