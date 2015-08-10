package com.yazino.platform.processor.statistic.achievement;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;

public class AchievementStatisticEventsConsumerTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String GAME_TYPE = "gameType";
    private static final StatisticEvent EVENT_1 = new StatisticEvent("event1");
    private static final StatisticEvent EVENT_2 = new StatisticEvent("event2");

    private Collection<Accumulator> accumulators;
    private AchievementRepository achievementRepository;
    private PlayerAchievementsRepository playerAchievementsRepository;
    private AchievementStatisticEventsConsumer underTest;

    private Achievement ACHIEVEMENT_1 = new Achievement("ACHIEVEMENT_1", 1, "a1Title", "a1Message", "a1ShortDesc",
            "how to get 1", "Posted Achievement Title 1", "", "", "", GAME_TYPE, set("event1"), "accumulator1", null, true);
    private Achievement ACHIEVEMENT_2 = new Achievement("ACHIEVEMENT_2", 1, "a2Title", "a2Message", "a2ShortDesc",
            "how to get 2", "Posted Achievement Title 2", "", "", "", GAME_TYPE, set("event2"), "accumulator2", null, true);

    private PlayerAchievements playerAchievements =
            new PlayerAchievements(PLAYER_ID, new HashSet<String>(), new HashMap<String, String>());
    private Accumulator accumulator1;
    private Accumulator accumulator2;

    @Before
    public void setUp() {
        accumulator1 = createAccumulator("accumulator1");
        accumulator2 = createAccumulator("accumulator2");
        accumulators = Arrays.asList(accumulator1, accumulator2);
        achievementRepository = mock(AchievementRepository.class);
        playerAchievementsRepository = mock(PlayerAchievementsRepository.class);
        underTest = new AchievementStatisticEventsConsumer(accumulators, achievementRepository, playerAchievementsRepository);
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenReturn(playerAchievements);
        when(achievementRepository.findByGameType(GAME_TYPE)).thenReturn(Arrays.asList(ACHIEVEMENT_1, ACHIEVEMENT_2));
    }

    private Accumulator createAccumulator(final String name) {
        final Accumulator mock = mock(Accumulator.class);
        when(mock.getName()).thenReturn(name);
        return mock;
    }

    @Test
    public void shouldCallAccumulators() {
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));
        verify(accumulator1).accumulate(playerAchievements, ACHIEVEMENT_1, EVENT_1);
        verify(accumulator2).accumulate(playerAchievements, ACHIEVEMENT_2, EVENT_2);
    }

    @Test
    public void shouldCallAccumulatorsEvenIfNoEventsMatch() {
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_2));
        verify(accumulator1).accumulate(playerAchievements, ACHIEVEMENT_1, (StatisticEvent) null);
        verify(accumulator2).accumulate(playerAchievements, ACHIEVEMENT_2, EVENT_2);
    }

    @Test
    public void shouldCallAccumulatorsEvenIfAchievementHasNoEventDefined() {
        ACHIEVEMENT_1.setEvents(new HashSet<String>());
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1));
        verify(accumulator1).accumulate(playerAchievements, ACHIEVEMENT_1, (StatisticEvent) null);
    }

    @Test
    public void shouldCallAccumulatorsWithMultipleRelevantEvents() {
        ACHIEVEMENT_1.setEvents(set(EVENT_1.getEvent(), EVENT_2.getEvent()));
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));
        verify(accumulator1).accumulate(playerAchievements, ACHIEVEMENT_1, EVENT_1, EVENT_2);
    }

    @Test
    public void shouldNotCallAccumulatorIfUsesMultipleEventsButNotAllOfThemMatch() {
        ACHIEVEMENT_1.setEvents(set(EVENT_1.getEvent(), EVENT_2.getEvent()));
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1));
        verify(accumulator1, never()).accumulate(eq(playerAchievements), any(Achievement.class), Matchers.<StatisticEvent>anyVararg());
    }

    @Test
    public void shouldSaveAchievementsIfChangeWasDetected() {
        when(accumulator1.accumulate(playerAchievements, ACHIEVEMENT_1, EVENT_1)).thenReturn(true);
        when(accumulator2.accumulate(playerAchievements, ACHIEVEMENT_2, EVENT_2)).thenReturn(false);
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));
        verify(playerAchievementsRepository).save(playerAchievements);
    }

    @Test
    public void shouldPerformNoFurtherActionOnAchievementsIfNoAccumulatorIsCalled() {
        ACHIEVEMENT_1.setEvents(set(EVENT_1.getEvent(), EVENT_2.getEvent()));
        ACHIEVEMENT_2.setEvents(set(EVENT_1.getEvent(), EVENT_2.getEvent()));
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1));
        verify(accumulator1, never()).accumulate(eq(playerAchievements), any(Achievement.class), Matchers.<StatisticEvent>anyVararg());
        verify(accumulator2, never()).accumulate(eq(playerAchievements), any(Achievement.class), Matchers.<StatisticEvent>anyVararg());
    }

    @Test
    public void shouldPerformNoFurtherActionOnAchievementsIfAccumulatorsDoNotChangeAnything() {
        when(accumulator1.accumulate(playerAchievements, ACHIEVEMENT_1, EVENT_1)).thenReturn(false);
        when(accumulator2.accumulate(playerAchievements, ACHIEVEMENT_2, EVENT_2)).thenReturn(false);
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));
    }

    @Test
    public void shouldNotCallAccumulatorAgainIfNotRecurringAndPlayerAlreadyHasAchievement() {
        ACHIEVEMENT_1.setRecurring(false);
        playerAchievements.awardAchievement(ACHIEVEMENT_1.getId());

        doThrow(new RuntimeException("This should not be called")).when(accumulator1).accumulate(
                any(PlayerAchievements.class), any(Achievement.class), any(StatisticEvent.class));

        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));

        verify(accumulator2).accumulate(playerAchievements, ACHIEVEMENT_2, EVENT_2);
        verify(accumulator1, never()).accumulate(eq(playerAchievements), eq(ACHIEVEMENT_1), any(StatisticEvent.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfUnknownAccumulator() {
        ACHIEVEMENT_1.setAccumulator("unknownAccumulator");
        underTest.processEvents(PLAYER_ID, GAME_TYPE, set(EVENT_1, EVENT_2));
    }

    private <T> Set<T> set(final T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }
}
