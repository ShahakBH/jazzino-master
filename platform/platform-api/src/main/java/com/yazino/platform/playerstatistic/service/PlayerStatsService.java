package com.yazino.platform.playerstatistic.service;

import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Set;

public interface PlayerStatsService {

    void publishExperience(@Routing BigDecimal playerId, String gameType);

    int getLevel(@Routing BigDecimal playerId, String gameType);

    LevelInfo getLevelInfo(@Routing BigDecimal playerId, String gameType);

    AchievementInfo getAchievementInfo(@Routing BigDecimal playerId, String gameType);

    Set<String> getPlayerAchievements(@Routing BigDecimal playerId);

    Set<AchievementDetails> getAchievementDetails(String gameType);

}
