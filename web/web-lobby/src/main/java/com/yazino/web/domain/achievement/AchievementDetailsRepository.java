package com.yazino.web.domain.achievement;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.web.service.PlayerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class AchievementDetailsRepository {
    private final PlayerStatsService playerStatsService;

    //cglib
    protected AchievementDetailsRepository() {
        playerStatsService = null;
    }

    @Autowired
    public AchievementDetailsRepository(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService may not be null");

        this.playerStatsService = playerStatsService;
    }

    @Cacheable(cacheName = "achievementDetailsCache")
    public Set<AchievementDetails> getAchievementDetails(final String gameType) {
        notNull(gameType, "gameType may not be null");

        return playerStatsService.getAchievementDetails(gameType);
    }
}
