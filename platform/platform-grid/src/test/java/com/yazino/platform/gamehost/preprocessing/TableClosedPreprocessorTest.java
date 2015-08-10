package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TableClosedPreprocessorTest {
    TableClosedPreprocessor underTest;
    ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new TableClosedPreprocessor(context.getDestinationFactory());
    }

    @Test
    public void if_the_table_is_closed_then_a_table_closed_document_is_sent_and_processing_halt_result_returned() throws GameException {
        context.getTable().setTableStatus(TableStatus.closed);
        boolean result = context.preprocess(underTest);
        assertTrue("Message 'the table is closed' is expected", context.getSinglePlayerDocument().getBody().contains("the table is closed"));
        assertFalse(result);
    }

    @Test
    public void if_the_table_is_not_closed_then_no_document_is_sent_andcontinue_result_is_returned() throws GameException {
        context.getTable().setTableStatus(TableStatus.open);
        boolean result = context.preprocess(underTest);
        context.verifyNoHostInteractions();
        assertTrue(result);
    }

    @Test
    public void if_the_table_is_closed_then_a_table_closed_processing_halt_result_returned_for_event() throws GameException {
        context.getTable().setTableStatus(TableStatus.closed);
        boolean result = context.preProcessEvent(underTest);
        context.verifyNoHostInteractions();
        assertFalse(result);
    }

    @Test
    public void if_the_table_is_closed_then_a_table_closed_and_nop_event_shouldnt_halt_result_returned_for_event() throws GameException {
        context.eventIsNopEvent();
        context.getTable().setTableStatus(TableStatus.closed);
        boolean result = context.preProcessEvent(underTest);
        context.verifyNoHostInteractions();
        assertTrue(result);
    }

    @Test
    public void if_the_table_is_not_closed_then_no_document_is_sent_andcontinue_result_is_returned_for_event() throws GameException {
        context.getTable().setTableStatus(TableStatus.open);
        boolean result = context.preProcessEvent(underTest);
        context.verifyNoHostInteractions();
        assertTrue(result);
    }

}
