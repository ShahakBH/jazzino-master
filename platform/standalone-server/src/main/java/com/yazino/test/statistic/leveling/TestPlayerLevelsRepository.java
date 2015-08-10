package com.yazino.test.statistic.leveling;

import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TestPlayerLevelsRepository implements PlayerLevelsRepository {
    private final Map<BigDecimal, PlayerLevels> levels = new HashMap<BigDecimal, PlayerLevels>();

    @Override
    public PlayerLevels forPlayer(final BigDecimal playerId) {
        return levels.get(playerId);
    }

    @Override
    public void save(final PlayerLevels playerLevels) {
        levels.put(playerLevels.getPlayerId(), playerLevels);
    }
}
