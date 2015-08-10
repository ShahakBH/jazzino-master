package com.yazino.platform.tournament;

import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Collection;

public interface TrophyLeaderboardService {
    BigDecimal create(TrophyLeaderboardDefinition trophyLeaderboard)
            throws TrophyLeaderboardException;

    void update(@Routing("getId") TrophyLeaderboardView trophyLeaderboard);

    void setIsActive(@Routing BigDecimal trophyLeaderboardId,
                     boolean isActive);

    Collection<TrophyLeaderboardView> findAll();

    TrophyLeaderboardView findById(@Routing BigDecimal trophyLeaderboardId);

    TrophyLeaderboardView findByGameType(String gameType);
}
