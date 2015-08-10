package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.*;

public class GameStatisticsPublishingPostProcessorTest {

    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(1);
    private static final String GAME_TYPE = "getGameType";
    private static final String CLIENT_ID = "clientId";

    private GameStatisticsPublisher publisher;
    private GameStatisticsPublishingPostProcessor underTest;
    private Table table;
    private GameStatus gameStatus = mock(GameStatus.class);
    private GameRules gameRules = mock(GameRules.class);

    @Before
    public void setUp() {
        publisher = mock(GameStatisticsPublisher.class);
        table = mock(Table.class);
        when(table.getTableId()).thenReturn(TABLE_ID);
        when(table.getGameTypeId()).thenReturn(GAME_TYPE);
        when(table.getClientId()).thenReturn(CLIENT_ID);
        underTest = new GameStatisticsPublishingPostProcessor(publisher);
    }

    @Test
    public void shouldPublishStatistics() {
        Collection<GameStatistic> stats = new ArrayList<GameStatistic>();
        ExecutionResult executionResult = new ExecutionResult.Builder(gameRules, gameStatus).gameStatistics(stats).build();
        underTest.postProcess(executionResult, null, table, null, null, null);
        verify(publisher).publish(TABLE_ID, GAME_TYPE, CLIENT_ID, stats);
    }

    @Test
    public void shouldIgnoreNullExecutionResult() {
        underTest.postProcess(null, null, table, null, null, null);
        verifyZeroInteractions(publisher);
    }
}
