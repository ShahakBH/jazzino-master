package com.yazino.platform.processor.table;

import com.yazino.game.api.ExternalGameService;
import com.yazino.game.api.GameStatus;
import com.yazino.platform.gamehost.GameInitialiser;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.preprocessing.EventPreprocessor;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.test.game.status.MockGameStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.yazino.platform.test.game.status.MockGameStatus.MockGamePhase.GameFinished;
import static com.yazino.platform.test.game.status.MockGameStatus.MockGamePhase.Playing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GameInitialiserTest {

    @Mock
    private TableBoundGamePlayerWalletFactory wallet;
    @Mock
    private ExternalGameService externalGameService;
    @Mock
    private com.yazino.game.api.GameRules gameRules;
    @Mock
    private Postprocessor postprocessor;

    private final GameInitialiser underTest = new GameInitialiser();

    @Before
    public void setup() {
        when(wallet.getAuditLabel()).thenReturn("TEST");
    }

    @Test
    public void shouldReturnFalseIfAPreProcessorFails() {
        Table table = tableWithPhase(Playing, 100L);
        when(wallet.getTable()).thenReturn(table);

        EventPreprocessor processor3 = mockEventPreprocessor(true);
        underTest.setEventPreInitialisationProcessors(Arrays.asList(mockEventPreprocessor(true), mockEventPreprocessor(false), processor3));
        boolean successful = underTest.runPreProcessors(gameRules, defaultEventContext());
        assertFalse(successful);
        assertEquals(Long.valueOf(100), table.getGameId());
        verifyZeroInteractions(processor3);
    }

    @Test
    public void shouldRunAllPreProcessorsBeforeExecuting() {
        Table table = tableWithPhase(Playing, 100L);
        when(wallet.getTable()).thenReturn(table);

        EventPreprocessor processor1 = mockEventPreprocessor(true);
        EventPreprocessor processor2 = mockEventPreprocessor(true);
        EventPreprocessor processor3 = mockEventPreprocessor(true);
        underTest.setEventPreInitialisationProcessors(toList(processor2, processor3, processor1));
        underTest.runPreProcessors(gameRules, defaultEventContext());
        verify(processor1).preprocess(any(com.yazino.game.api.ScheduledEvent.class), eq(table));
        verify(processor2).preprocess(any(com.yazino.game.api.ScheduledEvent.class), eq(table));
        verify(processor3).preprocess(any(com.yazino.game.api.ScheduledEvent.class), eq(table));
    }

    @Test
    public void shouldntRunPreprocessorsIfFlagSet() {
        Table table = tableWithPhase(Playing, 100L);
        when(wallet.getTable()).thenReturn(table);

        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        context.setRunPreProcessors(false);
        EventPreprocessor processor = mockEventPreprocessor(true);
        underTest.setEventPreInitialisationProcessors(toList(processor));
        underTest.initialiseGame(context);
        verifyZeroInteractions(processor);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void shouldUpdateGameIdWhenNewGameCreatedAndNotifyPostProcessors() {
        Table table = tableWithPhase(null, 100L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        Postprocessor processor = mockPostProcessor();
        underTest.setPostInitialisationProcessors(toList(processor));
        underTest.initialiseGame(context);
        assertEquals(Long.valueOf(1), table.getGameId());
        verify(processor).postProcess(any(com.yazino.game.api.ExecutionResult.class), any(com.yazino.game.api.Command.class), eq(table), anyString(), anyList(), eq(context.getPlayerId()));
    }

    @Test
    public void shouldIncrementGameIdWhenCreatedNextGameAndNotifyPostProcessors() {
        Table table = tableWithPhase(GameFinished, 6L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        context.setAllowedToMoveToNextGame(true);
        when(gameRules.isComplete(table.getCurrentGame())).thenReturn(true);
        underTest.setPostInitialisationProcessors(toList(postprocessor));

        underTest.initialiseGame(context);

        assertEquals(Long.valueOf(7), table.getGameId());
        verify(postprocessor).postProcess(any(com.yazino.game.api.ExecutionResult.class), any(com.yazino.game.api.Command.class), eq(table), anyString(), anyList(), eq(context.getPlayerId()));
    }

    @Test
    public void shouldCreateNewGameWhenNotAllowed() {
        Table table = tableWithPhase(GameFinished, 6L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        context.setAllowedToMoveToNextGame(false);
        underTest.initialiseGame(context);
        assertEquals(Long.valueOf(6), table.getGameId());
    }

    @Test
    public void shouldRunPostProcessorsWhenNewGameCreated() {
        Table table = tableWithPhase(null, 100L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        Postprocessor processor = mockPostProcessor();
        underTest.setPostInitialisationProcessors(toList(processor));
        underTest.initialiseGame(context);
        verify(processor).postProcess(any(com.yazino.game.api.ExecutionResult.class), any(com.yazino.game.api.Command.class), eq(table), eq("TEST"), anyList(), any(BigDecimal.class));

    }

    @Test
    public void shouldntRunPostProcessorsWhenDisabledAndNewGameCreated() {
        Table table = tableWithPhase(null, 100L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        context.setRunPostProcessors(false);
        Postprocessor processor = mockPostProcessor();
        underTest.setPostInitialisationProcessors(toList(processor));
        underTest.initialiseGame(context);
        assertEquals(Long.valueOf(1), table.getGameId());
        verifyZeroInteractions(processor);
    }

    @Test
    public void shouldntRunPostProcessorsWhenDisabledAndNextGameCreated() {
        Table table = tableWithPhase(GameFinished, 6L);
        when(wallet.getTable()).thenReturn(table);
        GameInitialiser.GameInitialisationContext context = defaultEventContext();
        context.setAllowedToMoveToNextGame(true);
        context.setRunPostProcessors(false);
        when(gameRules.isComplete(table.getCurrentGame())).thenReturn(true);
        underTest.setPostInitialisationProcessors(toList(postprocessor));

        underTest.initialiseGame(context);

        assertEquals(Long.valueOf(7), table.getGameId());
        verifyZeroInteractions(postprocessor);
    }

    private GameInitialiser.GameInitialisationContext defaultEventContext() {
        GameInitialiser.GameInitialisationContext initialisationContext = new GameInitialiser.GameInitialisationContext(new com.yazino.game.api.ScheduledEvent(0, 100, "", "", Collections.<String, String>emptyMap(), true), wallet, externalGameService, gameRules);
        initialisationContext.setAllowedToMoveToNextGame(false);
        return initialisationContext;
    }

    private static Table tableWithPhase(MockGameStatus.MockGamePhase phase, long gameId) {
        Table table = new Table(new com.yazino.game.api.GameType("SLOTS", "Slots", Collections.<String>emptySet()), BigDecimal.ONE, "DefaultSlots", true);
        table.setTableId(BigDecimal.ONE);
        if (phase != null) {
            table.setCurrentGame(new GameStatus(MockGameStatus.emptyStatus(new HashMap<String, String>()).withPhase(phase)));
        }
        table.setGameId(gameId);
        return table;
    }

    private static Postprocessor mockPostProcessor() {
        return mock(Postprocessor.class);
    }

    private static EventPreprocessor mockEventPreprocessor(boolean result) {
        EventPreprocessor processor = mock(EventPreprocessor.class);
        when(processor.preprocess(any(com.yazino.game.api.ScheduledEvent.class), any(Table.class))).thenReturn(result);
        return processor;
    }

    private static <T> List<T> toList(T... objects) {
        return Arrays.asList(objects);
    }
}
