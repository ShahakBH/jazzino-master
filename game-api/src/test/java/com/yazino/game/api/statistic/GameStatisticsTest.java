package com.yazino.game.api.statistic;

import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatistics;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameStatisticsTest {
    private GameStatistics underTest;
    private Collection<GameStatistic> stats;
    private GameStatistic stat2;
    private GameStatistic stat3;
    private GameStatistic stat1;

    @Before
    public void setUp() {
        stat1 = new GameStatistic(BigDecimal.valueOf(1), "stat1");
        stat2 = new GameStatistic(BigDecimal.valueOf(2), "stat2");
        stat3 = new GameStatistic(BigDecimal.valueOf(5), "stat2");
        stats = new ArrayList<GameStatistic>(Arrays.asList(stat1, stat2, stat3));
        underTest = new GameStatistics(stats);
    }

    @Test
    public void creates_iterator() {
        for (GameStatistic gameStatistic : underTest) {
            assertTrue(stats.contains(gameStatistic));
            stats.remove(gameStatistic);
        }
        assertTrue(stats.isEmpty());
    }

    @Test
    public void finds_specific_stats() {
        Collection<GameStatistic> expected = Arrays.asList(stat2, stat3);
        Collection<GameStatistic> actual = underTest.findByName("stat2");
        assertEquals(expected, actual);
    }

    @Test
    public void finds_unique_stat() {
        underTest = new GameStatistics(new ArrayList<GameStatistic>(Arrays.asList(stat1, stat2)));
        GameStatistic actual = underTest.findUniqueByName("stat2");
        assertEquals(stat2, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void finds_unique_stat_fail_if_not_unique() {
        Collection<GameStatistic> expected = Arrays.asList(stat2, stat3);
        GameStatistic actual = underTest.findUniqueByName("stat2");
        assertEquals(stat2, actual);
    }

    @Test
    public void check_if_stats_are_present() {
        assertTrue(underTest.contains("stat1", "stat2"));
        assertFalse(underTest.contains("stat8"));
    }
}
