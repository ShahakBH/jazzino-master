package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerLevels;

import java.math.BigDecimal;

public interface PlayerStatsDAO {

    PlayerLevels getLevels(BigDecimal playerId);

    void saveLevels(PlayerLevels playerLevels);

    PlayerAchievements getAchievements(BigDecimal playerId);

    void saveAchievements(PlayerAchievements playerAchievements);
}
