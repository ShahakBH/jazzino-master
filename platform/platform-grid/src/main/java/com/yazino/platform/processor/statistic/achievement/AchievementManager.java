package com.yazino.platform.processor.statistic.achievement;


import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;

import java.util.List;

public interface AchievementManager {

    /**
     * Award an achievement to a player.
     * <p/>
     * If the player already has the achievement then the appropriate action will be performed.
     *
     * @param playerAchievements the player
     * @param achievement        the achievement.
     * @param parameters         parameters to the achievement.
     * @param delay              delay on achievement notifications.
     */
    void awardAchievement(PlayerAchievements playerAchievements,
                          Achievement achievement,
                          List<Object> parameters,
                          long delay);

}
