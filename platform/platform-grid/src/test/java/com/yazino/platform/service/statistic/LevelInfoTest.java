package com.yazino.platform.service.statistic;

import com.yazino.platform.playerstatistic.service.LevelInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LevelInfoTest {

    @Test
    public void shouldCalculatePercentage() {
        assertEquals(52, new LevelInfo(1, 52, 100).getPercentage(), 0);
        assertEquals(0, new LevelInfo(1, 52, 0).getPercentage(), 0);
        assertEquals(100, new LevelInfo(1, 120, 100).getPercentage(), 0);
        assertEquals(0, new LevelInfo(1, 0, 800).getPercentage(), 0);
        assertEquals(30, new LevelInfo(1, 3, 10).getPercentage(), 0);
        assertEquals(74, new LevelInfo(1, 297, 399).getPercentage(), 0);
    }
}
