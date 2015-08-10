package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.web.service.PlayerStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("achievementInfoRepository")
public class AchievementInfoRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AchievementInfoRepository.class);

    private final PlayerStatsService playerStatsService;

    //cglib
    public AchievementInfoRepository() {
        playerStatsService = null;
    }

    @Autowired
    public AchievementInfoRepository(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService is null");
        this.playerStatsService = playerStatsService;
    }

    @Cacheable(cacheName = "achievementInfoCache")
    public AchievementInfo getAchievementInfo(final BigDecimal playerId, final String gameType) {
        try {
            return playerStatsService.getAchievementInfo(playerId, gameType);
        } catch (Exception e) {
            LOG.error(String.format("Could not retrieve achievements for player %s and gameType %s",
                    playerId, gameType), e);
            return null;
        }
    }
}
