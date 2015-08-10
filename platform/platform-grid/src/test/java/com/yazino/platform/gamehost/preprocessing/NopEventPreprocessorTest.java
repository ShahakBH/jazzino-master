package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.ScheduledEvent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NopEventPreprocessorTest {
    private NopEventPreprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void setUp() {
        underTest = new NopEventPreprocessor();
        context = new ProcessingTestContext();
    }

    @Test
    public void if_nop_event_halt_returned() {
        context.setEvent(ScheduledEvent.noProcessingEvent(0));
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_other_event_continue_returned() {
        final boolean result = context.preProcessEvent(underTest);
        assertTrue(result);
    }
}
