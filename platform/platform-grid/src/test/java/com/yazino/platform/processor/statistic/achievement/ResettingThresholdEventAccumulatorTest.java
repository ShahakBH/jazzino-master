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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResettingThresholdEventAccumulatorTest {
    private static final String PARAM_2 = "param2";
    private static final String PARAM_1 = "param1";
    private static final long DELAY = 334L;
    private static final int MULTIPLIER = 1;
    private static final String EVENT_NAME = "anEvent";
    private static final int THRESHOLD = 3;
    private static final String ACHIEVEMENT_ID = "aTestAchievement";

    @Mock
    private AchievementManager achievementManager;

    private ResettingThresholdEventAccumulator unit;
    private PlayerAchievements player;
    private Achievement achievement;
    private StatisticEvent event;

    @Before
    public void setUp() throws Exception {
        player = new PlayerAchievements(BigDecimal.valueOf(3454), new HashSet<String>(), new HashMap<String, String>());
        achievement = achievement(ACHIEVEMENT_ID, "resettingThresholdEvent", Integer.toString(THRESHOLD));
        event = new StatisticEvent(EVENT_NAME, DELAY, MULTIPLIER, PARAM_1, PARAM_2);

        unit = new ResettingThresholdEventAccumulator(achievementManager);
    }

    @Test
    public void ensureThatPeopleChangingTheNameReadThisTestAndRememberToUpdateTheDatabase() {
        assertThat(unit.getName(), is(equalTo("resettingThresholdEvent")));
    }

    @Test
    public void accumulatorStateIsUpdatedAfterCall() {
        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("")));

        boolean changed = unit.accumulate(player, achievement, event);
        assertTrue(changed);

        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("1")));
    }

    @Test
    public void achievementIsAwardedWhenThresholdIsReached() {
        for (int i = 0; i < THRESHOLD; ++i) {
            boolean changed = unit.accumulate(player, achievement, event);
            assertTrue(changed);
        }

        verify(achievementManager).awardAchievement(player, achievement, asList((Object) PARAM_1, PARAM_2), DELAY);
    }

    @Test
    public void achievementIsAwardedWhenThresholdIsExceeded() {
        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("")));
        event = new StatisticEvent(EVENT_NAME, DELAY, multiplier(THRESHOLD * 2), PARAM_1, PARAM_2);

        unit.accumulate(player, achievement, event);

        verify(achievementManager).awardAchievement(player, achievement, asList((Object) PARAM_1, PARAM_2), DELAY);
    }

    @SuppressWarnings({"unchecked", "ThrowableInstanceNeverThrown"})
    @Test
    public void achievementProgressIsResetIfNullEventIsPassed() {
        doThrow(new RuntimeException("This should not be called")).when(achievementManager).awardAchievement(
                any(PlayerAchievements.class), any(Achievement.class), any(List.class), anyLong());

        for (int i = 0; i < THRESHOLD - 1; ++i) {
            boolean changed = unit.accumulate(player, achievement, event);
            assertTrue(changed);
        }

        assertThat(player.progressForAchievement(achievement.getId()), is(equalTo(Integer.toString(THRESHOLD - 1))));

        boolean changed = unit.accumulate(player, achievement);
        assertTrue(changed);

        assertThat(player.progressForAchievement(achievement.getId()), is(equalTo("0")));
    }

    @Test
    public void achievementIsNotAwardedWhenThresholdIsNotReached() {
        verifyNoMoreInteractions(achievementManager);

        for (int i = 0; i < THRESHOLD - 1; ++i) {
            unit.accumulate(player, achievement, event);
        }
    }

    @Test
    public void achievementIsNotAwardedWhenThresholdWasAlreadyReached() {
        player.getAchievementProgress().put(ACHIEVEMENT_ID, Integer.toString(THRESHOLD));
        event = new StatisticEvent(EVENT_NAME, DELAY, multiplier(1), PARAM_1, PARAM_2);

        unit.accumulate(player, achievement, event);

        verifyZeroInteractions(achievementManager);
    }

    @Test
    public void achievementIsNotAwardedWhenThresholdWasAlreadyExceeded() {
        player.getAchievementProgress().put(ACHIEVEMENT_ID, Integer.toString(THRESHOLD + 1));
        event = new StatisticEvent(EVENT_NAME, DELAY, multiplier(1), PARAM_1, PARAM_2);

        unit.accumulate(player, achievement, event);

        verifyZeroInteractions(achievementManager);
    }

    @Test
    public void multiplierDeterminesAmountOfProgressAccumulated() {
        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("")));
        int multiplier = 2;
        event = new StatisticEvent(EVENT_NAME, DELAY, multiplier, PARAM_1, PARAM_2);
        unit.accumulate(player, achievement, event);

        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("2")));
    }

    @Test
    public void multiplierOverriddenToOneWhenInvokedWithMultipleEvents() {
        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("")));
        StatisticEvent event1 = new StatisticEvent(EVENT_NAME, DELAY, multiplier(2), PARAM_1, PARAM_2);
        StatisticEvent event2 = new StatisticEvent(EVENT_NAME, DELAY, multiplier(3), PARAM_1, PARAM_2);
        unit.accumulate(player, achievement, event1, event2);

        assertThat(player.progressForAchievement(ACHIEVEMENT_ID), is(equalTo("1")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionIsThrownWhenMultiplierLessThanOne() {
        int multiplier = 0;
        event = new StatisticEvent(EVENT_NAME, DELAY, multiplier, PARAM_1, PARAM_2);
        unit.accumulate(player, achievement, event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionIsThrownOnInvalidParameters() {
        achievement = achievement(ACHIEVEMENT_ID, "resettingThresholdEvent", "bob");

        unit.accumulate(player, achievement, event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionIsThrownOnInvalidState() {
        player.updateProgressForAchievement(ACHIEVEMENT_ID, "bob");

        unit.accumulate(player, achievement, event);
    }

    @Test
    public void nullEventIsIgnored() {
        verifyNoMoreInteractions(achievementManager);

        unit.accumulate(player, achievement);
    }

    private Achievement achievement(String id, String thresholdAccumulator, String threshold) {
        return new Achievement(id, 1, "title", "msg", "shortDesc", "howToGet", "Achieved", "blackjack", "Play blackjack", "blackjack", "gameType", Collections.<String>emptySet(), thresholdAccumulator, threshold, true);
    }

    private int multiplier(int value) {
        return value;
    }
}
