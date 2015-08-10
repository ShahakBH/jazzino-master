package com.yazino.platform.service.statistic;

import com.yazino.platform.playerstatistic.service.PlayerStatsBackOfficeService;
import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingPlayerStatsBackOfficeService implements PlayerStatsBackOfficeService {

    private final AchievementRepository achievementRepository;
    private final LevelingSystemRepository levelingSystemRepository;

    @Autowired
    public GigaspaceRemotingPlayerStatsBackOfficeService(final AchievementRepository achievementRepository,
                                                         final LevelingSystemRepository levelingSystemRepository) {
        notNull(achievementRepository, "achievementRepository is null");
        notNull(levelingSystemRepository, "levelingSystemRepository is null");

        this.achievementRepository = achievementRepository;
        this.levelingSystemRepository = levelingSystemRepository;
    }

    @Override
    public void refreshAchievements() {
        achievementRepository.refreshDefinitions();
    }

    @Override
    public void refreshLevelDefinitions() {
        levelingSystemRepository.refreshDefinitions();
    }
}
