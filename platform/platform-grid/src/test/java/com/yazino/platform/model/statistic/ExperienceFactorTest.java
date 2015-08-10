package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExperienceFactorTest {
    private static final String EVENT_NAME = "myEvent";
    private static final BigDecimal POINTS = BigDecimal.ONE;
    private static final int MULTIPLIER = 1;
    private ExperienceFactor underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new ExperienceFactor(EVENT_NAME, POINTS);
    }

    @Test
    public void shouldGiveExperienceIfEventIsPresent() {
        final BigDecimal result = underTest.calculateExperiencePoints(Arrays.asList(new StatisticEvent(EVENT_NAME), new StatisticEvent("some other event")));
        assertEquals(POINTS, result);
    }

    @Test
    public void shouldMultiplyExperienceIfEventContainsMultiplier() {
        final BigDecimal result = underTest.calculateExperiencePoints(Arrays.asList(new StatisticEvent(EVENT_NAME, 0, MULTIPLIER, 3), new StatisticEvent("some other event")));
        assertEquals(POINTS.multiply(BigDecimal.valueOf(3)), result);
    }

    @Test
    public void shouldNotMultiplyExperienceIfMultiplierIsInvalid() {
        final BigDecimal result = underTest.calculateExperiencePoints(Arrays.asList(new StatisticEvent(EVENT_NAME, 0, MULTIPLIER, "3"), new StatisticEvent("some other event")));
        assertEquals(POINTS, result);
    }


}
