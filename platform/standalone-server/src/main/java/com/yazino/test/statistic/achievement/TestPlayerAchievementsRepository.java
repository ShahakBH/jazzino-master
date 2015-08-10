package com.yazino.test.statistic.achievement;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TestPlayerAchievementsRepository implements PlayerAchievementsRepository {
    private final Map<BigDecimal, PlayerAchievements> players = new HashMap<BigDecimal, PlayerAchievements>();

    @Override
    public PlayerAchievements forPlayer(final BigDecimal playerId) {
        return players.get(playerId);
    }

    @Override
    public void save(final PlayerAchievements playerAchievements) {
        players.put(playerAchievements.getPlayerId(), playerAchievements);
    }
}
