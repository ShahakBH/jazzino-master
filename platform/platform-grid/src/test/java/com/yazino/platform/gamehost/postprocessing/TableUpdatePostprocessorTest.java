package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableUpdatePostprocessorTest {
    private TableUpdatePostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void setUp() {
        context = new ProcessingTestContext();
        underTest = new TableUpdatePostprocessor(context.getGameRepository(), context.getTimeSource());
    }

    @Test
    public void updates_table_with_new_game_status() throws GameException {
        context.postprocess(underTest);
        Assert.assertEquals(1L, context.getTable().getIncrement().longValue());
        Assert.assertEquals(context.getNewGameStatus(), context.getTable().getCurrentGame());
        Assert.assertEquals(1, context.getTable().getScheduledEventWrappers().size());
        Assert.assertEquals(4, context.getTable().getFreeSeats().intValue());
    }

    @Test
    public void does_nothing_if_execution_result_not_present() throws GameException {
        context.setExecutionResult(null);
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void ensureTableAttributeAvailableForPlayersJoiningFromGameStatusWhenAvailable() throws GameException {
        context.updateGameAvailableForPlayersJoining();
        context.postprocess(underTest);
        context.verifyAvaialbeForPlayersJoining();
        assertTrue(context.getTable().getAvailableForPlayersJoining());
    }

    @Test
    public void ensureTableAttributeAvailableForPlayersJoiningFromGameStatusWhenNotAvailable() throws GameException {
        context.updateGameNotAvailableForPlayersJoining();
        context.postprocess(underTest);
        context.verifyAvaialbeForPlayersJoining();
        assertFalse(context.getTable().getAvailableForPlayersJoining());
    }
}
