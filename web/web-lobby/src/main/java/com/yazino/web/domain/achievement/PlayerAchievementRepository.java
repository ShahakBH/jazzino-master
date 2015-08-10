package com.yazino.web.domain.achievement;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.web.service.PlayerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class PlayerAchievementRepository {
    private final PlayerStatsService playerStatsService;

    //cglib
    protected PlayerAchievementRepository() {
        playerStatsService = null;
    }

    @Autowired
    public PlayerAchievementRepository(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService may not be null");

        this.playerStatsService = playerStatsService;
    }

    @Cacheable(cacheName = "playerAchievementCache")
    public Set<String> getPlayerAchievements(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        return playerStatsService.getPlayerAchievements(playerId);
    }
}
