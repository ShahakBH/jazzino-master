package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.service.statistic.StatisticEventService;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PlayerStatisticEventPublishingProcessorTest {
    private StatisticEventService service;
    private PlayerStatisticEventPublishingProcessor underTest;

    @Before
    public void setUp() {
        service = mock(StatisticEventService.class);
        underTest = new PlayerStatisticEventPublishingProcessor(service);
    }

    @Test
    public void shouldConvertAndSendMessage() {
        final PlayerStatisticEvent request = new PlayerStatisticEvent(BigDecimal.ONE,
                "gameType",
                Arrays.<StatisticEvent>asList(
                        new StatisticEvent("evt1", 1, 1, "evt1par1", "evt1par2"),
                        new StatisticEvent("evt2", 2, 2, "evt1par2", "evt2par2")
                ));
        underTest.processRequest(request);
        final Collection<com.yazino.platform.playerstatistic.StatisticEvent> expectedEvents = Arrays.asList(
                new com.yazino.platform.playerstatistic.StatisticEvent("evt1", 1, 1, "evt1par1", "evt1par2"),
                new com.yazino.platform.playerstatistic.StatisticEvent("evt2", 2, 2, "evt1par2", "evt2par2")
        );
        verify(service).publishStatistics(BigDecimal.ONE, "gameType", expectedEvents);
    }
}
