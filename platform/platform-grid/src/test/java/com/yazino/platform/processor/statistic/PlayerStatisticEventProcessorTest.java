package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerStatistics;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class PlayerStatisticEventProcessorTest {

    private PlayerStatisticEventConsumer consumer1;
    private PlayerStatisticEventConsumer consumer2;
    private PlayerStatisticEventProcessor underTest;
    private PlayerStatistics message;

    @Before
    public void setUp() {
        message = new PlayerStatistics(BigDecimal.valueOf(1), "gameType", Collections.<StatisticEvent>emptyList());
        consumer1 = mock(PlayerStatisticEventConsumer.class);
        consumer2 = mock(PlayerStatisticEventConsumer.class);
        underTest = new PlayerStatisticEventProcessor(Arrays.asList(consumer1, consumer2));
    }

    @Test
    public void shouldInvokeAllConsumers() {
        underTest.process(message);
        verify(consumer1).processEvents(message.getPlayerId(), message.getGameType(), message.getEvents());
        verify(consumer2).processEvents(message.getPlayerId(), message.getGameType(), message.getEvents());
    }

    @Test
    public void shouldInvokeNextConsumerInCaseOfException() {
        doThrow(new RuntimeException("error")).when(consumer1).processEvents(message.getPlayerId(), message.getGameType(), message.getEvents());
        underTest.process(message);
        verify(consumer2).processEvents(message.getPlayerId(), message.getGameType(), message.getEvents());
    }

    @Test
    public void shouldNotInvokeConsumersIfMessageNotPresent() {
        underTest.process(null);
        verifyZeroInteractions(consumer1, consumer2);
    }
}
