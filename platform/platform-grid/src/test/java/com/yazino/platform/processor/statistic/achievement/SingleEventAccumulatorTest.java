package com.yazino.platform.processor.statistic.achievement;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SingleEventAccumulatorTest {
    private static final String PARAM_2 = "param2";
    private static final String PARAM_1 = "param1";
    private static final long DELAY = 334L;
    private static final int MULTIPLIER = 1;
    private static final String EVENT_NAME = "anEvent";

    @Mock
    private AchievementManager achievementManager;

    private PlayerAchievements player;
    private Achievement achievement;
    private SingleEventAccumulator unit;
    private StatisticEvent statisticEvent;

    @Before
    public void setUp() throws Exception {
        player = new PlayerAchievements(BigDecimal.valueOf(3454), Collections.<String>emptySet(), Collections.<String, String>emptyMap());
        achievement = achievement("anAchievement");
        statisticEvent = new StatisticEvent(EVENT_NAME, DELAY, MULTIPLIER, PARAM_1, PARAM_2);

        unit = new SingleEventAccumulator(achievementManager);
    }

    @Test
    public void ensureThatPeopleChangingTheNameReadThisTestAndRememberToUpdateTheDatabase() {
        assertThat(unit.getName(), is(equalTo("singleEvent")));
    }

    @Test
    public void accumulationAwardsTheAchievementToThePlayer() {
        boolean changed = unit.accumulate(player, achievement, statisticEvent);
        assertTrue(changed);
        verify(achievementManager).awardAchievement(player, achievement, Arrays.asList((Object) PARAM_1, PARAM_2), DELAY);
    }

    @Test
    public void nullEventIsIgnored() {
        boolean changed = unit.accumulate(player, achievement);
        assertFalse(changed);
        verifyZeroInteractions(achievementManager);
    }

    private Achievement achievement(String id) {
        return new Achievement(id, 1, "title", "msg", "shortDesc", "howToGet", "Achieved", "blackjack", "Play blackjack", "blackjack", "gameType", Collections.<String>emptySet(), "acc", "accPar", true);
    }
}
