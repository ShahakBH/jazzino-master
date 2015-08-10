package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableClosingPreprocessorTest {

    TableClosingPreprocessor underTest;
    ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new TableClosingPreprocessor(context.getDestinationFactory());
    }

    @Test
    public void if_table_is_open_continue() throws GameException {
        context.getTable().setTableStatus(TableStatus.open);
        final boolean result = context.preprocess(underTest);
        assertTrue(result);
    }

    @Test
    public void if_table_is_closing_and_player_is_not_at_table_a_document_is_sent_and_processing_halt_result_returned() throws GameException {
        context.getTable().setTableStatus(TableStatus.closing);
        runTestForDocumentExpected();
    }

    private void runTestForDocumentExpected() throws GameException {
        final boolean result = context.preprocess(underTest);
        assertTrue("Message 'this table is closing' is expected", context.getSinglePlayerDocument().getBody().contains("this table is closing"));
        assertFalse(result);
    }
}
