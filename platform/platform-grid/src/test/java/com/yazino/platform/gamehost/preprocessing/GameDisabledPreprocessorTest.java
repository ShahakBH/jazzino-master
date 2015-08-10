package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameDisabledPreprocessorTest {
    GameDisabledPreprocessor underTest;
    ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new GameDisabledPreprocessor(context.getGameRepository(), context.getDestinationFactory());
    }

    @Test
    public void if_game_disabled_and_current_game_complete_halts_execution() throws GameException {
        context.setGameEnabled(false);
        context.gameIsFinished();
        final boolean result = context.preprocess(underTest);
        assertFalse(result);
    }

    @Test
    public void if_game_disabled_and_player_not_at_table_sends_document_and_halts_execution() throws GameException {
        context.playerIsPlaying();
        context.setGameEnabled(false);
        context.gameCanBeClosed();
        final boolean result = context.preprocess(underTest);
        Assert.assertTrue("Message 'This game has been disabled' is expected", context.getSinglePlayerDocument().getBody().contains("This game has been disabled"));
        assertFalse(result);
    }

    @Test
    public void if_game_enabled_continue_execution() throws GameException {
        context.setGameEnabled(true);
        final boolean result = context.preprocess(underTest);
        assertTrue(result);
    }

    @Test
    public void if_game_disabled_but_game_incomplete_and_player_not_at_table_halts_execution() throws GameException {
        context.setGameEnabled(false);
        final boolean result = context.preprocess(underTest);
        Assert.assertTrue("Message 'This game has been disabled' is expected", context.getSinglePlayerDocument().getBody().contains("This game has been disabled"));
        assertFalse(result);
    }

    @Test
    public void if_game_disabled_but_game_incomplete_and_player_at_table_continue_execution() throws GameException {
        context.playerIsPlaying();
        context.setGameEnabled(false);
        final boolean result = context.preprocess(underTest);
        assertTrue(result);
    }

    @Test
    public void if_game_disabled_and_current_game_complete_halts_execution_for_event() throws GameException {
        context.setGameEnabled(false);
        context.gameCanBeClosed();
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_game_disabled_and_nop_event_continue_execution_for_event() throws GameException {
        context.eventIsNopEvent();
        context.setGameEnabled(true);
        final boolean result = context.preProcessEvent(underTest);
        assertTrue(result);
    }

    @Test
    public void if_game_enabled_continue_execution_for_event() throws GameException {
        context.setGameEnabled(true);
        final boolean result = context.preProcessEvent(underTest);
        assertTrue(result);
    }
}
