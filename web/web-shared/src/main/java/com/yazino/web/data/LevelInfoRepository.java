package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.playerstatistic.service.LevelInfo;
import com.yazino.web.service.PlayerStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("levelInfoRepository")
public class LevelInfoRepository {
    private static final Logger LOG = LoggerFactory.getLogger(LevelInfoRepository.class);
    private final PlayerStatsService playerStatsService;

    //cglib
    protected LevelInfoRepository() {
        this.playerStatsService = null;
    }

    @Autowired
    public LevelInfoRepository(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService is null");
        this.playerStatsService = playerStatsService;
    }

    @Cacheable(cacheName = "levelInfoCache")
    public LevelInfo getLevelInfo(final BigDecimal playerId, final String gameType) {
        try {
            return playerStatsService.getLevelInfo(playerId, gameType);
        } catch (Exception e) {
            LOG.error(String.format("Could not retrieve level info for player %s and gameType %s",
                    playerId, gameType), e);
            return null;
        }
    }
}
