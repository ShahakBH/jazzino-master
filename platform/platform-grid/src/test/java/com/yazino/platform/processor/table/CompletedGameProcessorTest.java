package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.GameCompleted;
import com.yazino.platform.repository.statistic.PlayerGameStatisticProducerRepository;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatisticProducer;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CompletedGameProcessorTest {
    private CompletedGameProcessor underTest;
    @Mock
    private GameStatisticProducer statisticsStrategy1;
    @Mock
    private GameStatisticProducer statisticsStrategy2;
    @Mock
    private GameStatus gameStatus;
    @Mock
    private GameStatisticsPublisher publisher;
    private GameStatistic gameStatistic1;
    private GameStatistic gameStatistic2;
    private String gameType = "GAME_TYPE";
    private String otherGameType = "OTHER_GAME_TYPE";
    private BigDecimal tableId = BigDecimal.TEN;
    private BigDecimal playerId1;
    private BigDecimal playerId2;
    private String clientId = "CLIENT_ID";

    @Before
    public void init() {
        final PlayerGameStatisticProducerRepository producerRepository = mock(PlayerGameStatisticProducerRepository.class);
        when(producerRepository.getProducers()).thenReturn(asList(statisticsStrategy1, statisticsStrategy2));

        underTest = new CompletedGameProcessor(publisher, producerRepository);
        playerId1 = BigDecimal.valueOf(1);
        playerId2 = BigDecimal.valueOf(2);
        gameStatistic1 = new GameStatistic(playerId1, "stat1");
        gameStatistic2 = new GameStatistic(playerId2, "stat2");
        when(statisticsStrategy1.processCompletedGame(gameStatus)).thenReturn(asList(gameStatistic1));
        when(statisticsStrategy2.processCompletedGame(gameStatus)).thenReturn(asList(gameStatistic2));
        when(statisticsStrategy1.getGameType()).thenReturn(gameType);
        when(statisticsStrategy2.getGameType()).thenReturn(otherGameType);
    }

    @Test
    public void all_strategies_are_delegated_to() {
        GameCompleted completedGame = new GameCompleted(gameStatus, gameType, tableId, clientId);
        GameCompleted otherCompletedGame = new GameCompleted(gameStatus, otherGameType, tableId, clientId);
        underTest.process(completedGame);
        verify(statisticsStrategy1).processCompletedGame(gameStatus);
        underTest.process(otherCompletedGame);
        verify(statisticsStrategy2).processCompletedGame(gameStatus);
    }

    @Test
    public void statistics_are_published() {
        underTest.process(new GameCompleted(gameStatus, gameType, tableId, clientId));
        verify(publisher).publish(tableId, gameType, clientId, asList(gameStatistic1));
        underTest.process(new GameCompleted(gameStatus, otherGameType, tableId, clientId));
        verify(publisher).publish(tableId, otherGameType, clientId, asList(gameStatistic2));
    }

    @Test
    public void only_strategies_for_game_type_are_delegated_to() {
        underTest.process(new GameCompleted(gameStatus, gameType, tableId, clientId));
        verify(statisticsStrategy1).processCompletedGame(gameStatus);
        verify(statisticsStrategy2, never()).processCompletedGame(any(GameStatus.class));
    }
}
