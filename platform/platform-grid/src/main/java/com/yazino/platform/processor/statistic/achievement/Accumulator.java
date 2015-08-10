package com.yazino.platform.processor.statistic.achievement;


import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;

public interface Accumulator {

    /**
     * Get the name of the accumulator.
     * <p/>
     * This is used by the data to refer to the accumulator.
     *
     * @return the name.
     */
    String getName();

    /**
     * Accumulate an event(s), or lack of.
     *
     * @param playerAchievements
     * @param achievement        the achievement.
     * @param events             the events, or null if the event was absent.
     * @return true if the accumulator affected the Player
     */
    boolean accumulate(PlayerAchievements playerAchievements,
                       Achievement achievement,
                       StatisticEvent... events);

}
