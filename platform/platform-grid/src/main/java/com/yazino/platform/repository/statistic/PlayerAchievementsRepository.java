package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;

import java.math.BigDecimal;

public interface PlayerAchievementsRepository {
    PlayerAchievements forPlayer(BigDecimal playerId);

    void save(PlayerAchievements playerAchievements);
}
