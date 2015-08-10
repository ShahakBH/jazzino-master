package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AchievementTest {
    private static final StatisticEvent MATCHING_EVENT = new StatisticEvent("aMatchingEvent");
    private static final StatisticEvent OTHER_EVENT = new StatisticEvent("anotherEvent");

    private Achievement achievement;
    private Set<String> events = new HashSet<String>();

    @Before
    public void setUp() {
        achievement = new Achievement("aTestAchievement", 1, "title", "message", "desc", "howToGet", "Achieved", "blackjack", "Play blackjack", "blackjack", "gameType", events, "acc", "accPar", true);
        events.addAll(set(MATCHING_EVENT.getEvent()));
    }

    @Test
    public void achievementAcceptsSingleMatchingEvent() {
        assertTrue(achievement.accepts(MATCHING_EVENT));
    }

    @Test
    public void achievementDoesNotAcceptOtherEvent() {
        events.clear();
        events.addAll(set(MATCHING_EVENT.getEvent(), OTHER_EVENT.getEvent()));

        assertFalse(achievement.accepts(OTHER_EVENT, OTHER_EVENT));
    }

    @Test
    public void achievementChecksEachEventsIsMatched() {
        assertFalse(achievement.accepts());
    }

    @Test
    public void achievementAcceptsMultipleMatchingEvents() {
        final StatisticEvent anotherMatchingEvent = new StatisticEvent("anotherMatchingEvent");
        events.clear();
        events.addAll(set(MATCHING_EVENT.getEvent(), anotherMatchingEvent.getEvent()));

        assertTrue(achievement.accepts(MATCHING_EVENT, anotherMatchingEvent));
    }

    @Test
    public void achievementDoesNotAcceptPartialMatchingEvents() {
        final StatisticEvent anotherMatchingEvent = new StatisticEvent("anotherMatchingEvent");
        events.clear();
        events.addAll(set(MATCHING_EVENT.getEvent(), anotherMatchingEvent.getEvent()));

        assertFalse(achievement.accepts(MATCHING_EVENT));
    }

    @Test
    public void achievementWithMultipleEventsDoesNotAcceptOtherEvent() {
        final StatisticEvent anotherMatchingEvent = new StatisticEvent("anotherMatchingEvent");
        events.clear();
        events.addAll(set(MATCHING_EVENT.getEvent(), anotherMatchingEvent.getEvent()));

        assertFalse(achievement.accepts(OTHER_EVENT));
    }

    private <T> Set<T> set(final T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }
}
