package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;
import com.yazino.game.api.ScheduledEvent;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GameCompletePostprocessorTest {
    private GameCompletePostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void setUp() {
        context = new ProcessingTestContext();
        underTest = new GameCompletePostprocessor(context.getGameRepository(), context.getTimeSource());
    }

    @Test
    public void if_game_is_not_complete_does_nothing() throws GameException {
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void if_game_is_complete_audit_and_schedule_nop_event() throws GameException {
        context.updateGameIsFinished();
        context.postprocess(underTest);
        context.moveTime(ScheduledEvent.NOP_TIMEOUT);
        final List<ScheduledEvent> tableEvents = context.getTableEvents();
        assertEquals(1, tableEvents.size());
        assertEquals(ScheduledEvent.NO_OP_EVENT, tableEvents.get(0).getEventSimpleName());
    }

}
