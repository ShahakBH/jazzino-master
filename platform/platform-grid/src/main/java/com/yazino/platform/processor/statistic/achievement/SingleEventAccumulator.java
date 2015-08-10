package com.yazino.platform.processor.statistic.achievement;


import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("achievementAccumulators")
public class SingleEventAccumulator implements Accumulator {
    private static final String NAME = "singleEvent";

    private final AchievementManager achievementManager;

    @Autowired
    public SingleEventAccumulator(@Qualifier("achievementManager") final AchievementManager achievementManager) {
        notNull(achievementManager, "Achievement Manager may not be null");
        this.achievementManager = achievementManager;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean accumulate(final PlayerAchievements player,
                              final Achievement achievement,
                              final StatisticEvent... event) {
        notNull(player, "Player may not be null");
        notNull(achievement, "Achievement may not be null");

        if (event != null && event.length > 0) {
            final StatisticEvent messageEvent = findBestMessageMatch(event);
            achievementManager.awardAchievement(player, achievement,
                    messageEvent.getParameters(), messageEvent.getDelay());
            return true;
        }
        return false;
    }

    private StatisticEvent findBestMessageMatch(final StatisticEvent... events) {
        StatisticEvent match = null;

        for (StatisticEvent event : events) {
            if (match == null || match.getParameters().size() < event.getParameters().size()) {
                match = event;
            }
        }

        return match;
    }
}
