package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.web.service.PlayerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("levelRepository")
public class LevelRepository {
    private final PlayerStatsService playerStatsService;

    //cglib
    protected LevelRepository() {
        this.playerStatsService = null;
    }

    @Autowired
    public LevelRepository(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService is null");
        this.playerStatsService = playerStatsService;
    }

    @Cacheable(cacheName = "levelCache")
    public Integer getLevel(final BigDecimal playerId,
                            final String gameType) {
        return playerStatsService.getLevel(playerId, gameType);
    }

}
