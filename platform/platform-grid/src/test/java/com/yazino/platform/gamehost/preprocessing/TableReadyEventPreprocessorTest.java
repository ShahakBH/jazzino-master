package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableReadyEventPreprocessorTest {
    private TableReadyEventPreprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void setUp() {
        context = new ProcessingTestContext();
        underTest = new TableReadyEventPreprocessor(context.getGameRepository());
    }

    @Test
    public void if_table_closed_halt_is_returned() {
        context.getTable().setTableStatus(TableStatus.closed);
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_table_has_no_game_status_halt_is_returned() {
        context.getTable().setCurrentGame(null);
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_current_game_is_completed_halt_is_returned() {
        context.gameIsFinished();
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_table_open_and_game_not_finished_continue_is_returned() {
        final boolean result = context.preProcessEvent(underTest);
        assertTrue(result);
    }
}
