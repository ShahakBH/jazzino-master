package com.yazino.platform.processor.statistic.achievement;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("achievementAccumulators")
public class ResettingThresholdEventAccumulator implements Accumulator {
    private static final String NAME = "resettingThresholdEvent";

    private final AchievementManager achievementManager;

    @Autowired
    public ResettingThresholdEventAccumulator(
            @Qualifier("achievementManager") final AchievementManager achievementManager) {
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

        int counter = getCounter(player, achievement);

        if (event == null || event.length == 0) {
            counter = 0;

        } else {
            final int threshold = getThreshold(achievement);
            final boolean originalCounterBelowThreshold = counter < threshold;
            final int multiplier;
            if (event.length == 1) {
                multiplier = event[0].getMultiplier();
            } else {
                multiplier = 1;
            }
            if (multiplier < 1) {
                throw new IllegalArgumentException("Multiplier must be >= 1");
            }

            counter += multiplier;

            if (originalCounterBelowThreshold && counter >= threshold) {
                final StatisticEvent messageEvent = findBestMessageMatch(event);
                achievementManager.awardAchievement(player, achievement,
                        messageEvent.getParameters(), messageEvent.getDelay());
            }
        }

        player.updateProgressForAchievement(achievement.getId(), Integer.toString(counter));
        return true;
    }

    private int getCounter(final PlayerAchievements player,
                           final Achievement achievement) {
        final String state = player.progressForAchievement(achievement.getId());
        if (StringUtils.isNotBlank(state)) {
            try {
                return Integer.parseInt(state);

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Achievement Progress must be an integer", e);
            }
        }

        return 0;
    }

    private int getThreshold(final Achievement achievement) {
        if (StringUtils.isBlank(achievement.getAccumulator())) {
            throw new IllegalArgumentException("Accumulator parameters must be supplied");
        }

        try {
            return Integer.valueOf(achievement.getAccumulatorParameters());

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Accumulator Parameters must be an integer", e);
        }
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
