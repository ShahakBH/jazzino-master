package com.yazino.web.service;

import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.platform.playerstatistic.service.LevelInfo;

import java.math.BigDecimal;
import java.util.Set;

public interface PlayerStatsService {

    int getLevel(BigDecimal playerId, String gameType);

    LevelInfo getLevelInfo(BigDecimal playerId, String gameType);

    AchievementInfo getAchievementInfo(BigDecimal playerId, String gameType);

    Set<String> getPlayerAchievements(BigDecimal playerId);

    Set<AchievementDetails> getAchievementDetails(String gameType);
}
