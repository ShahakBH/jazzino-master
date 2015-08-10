package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.ScheduledEvent;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventStillValidPreprocessorTest {
    EventStillValidPreprocessor underTest;
    ProcessingTestContext context;

    @Before
    public void init() {
        underTest = new EventStillValidPreprocessor();
        context = new ProcessingTestContext();
    }

    @Test
    public void if_old_event_halt_returned() {
        context.setEvent(new ScheduledEvent(0, context.getTable().getGameId() - 1, "", "", new HashMap<String, String>(), false));
        final boolean result = context.preProcessEvent(underTest);
        assertFalse(result);
    }

    @Test
    public void if_other_event_continue_returned() {
        final boolean result = context.preProcessEvent(underTest);
        assertTrue(result);
    }
}
