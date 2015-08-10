package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class AcknowledgementPreprocessorTest {
    AcknowledgementPreprocessor underTest;
    ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new AcknowledgementPreprocessor();
    }

    @Test
    public void when_command_type_is_game_continue_result_is_returned() throws GameException {
        final boolean result = context.preprocess(underTest);
        assertTrue(result);
    }

    @Test
    public void when_command_type_is_ack_halt_result_is_returned_and_table_state_is_updated() throws GameException {
        context.setCommand(new Command(context.getPlayer(), context.getTable().getTableId(),
                context.getTable().getGameId(), "uuid", Command.CommandType.Ack.getCode(), "10"));
        context.playerIsPlaying();

        final boolean result = context.preprocess(underTest);

        assertFalse(result);
        final Long actual = context.getTable().playerAtTable(context.getPlayer().getId()).getAcknowledgedIncrement();
        assertEquals(10L, actual.longValue());
    }

    @Test
    public void aMalformedAckIsIgnoredAndFalseIsReturned() throws GameException {
        context.getTable().playerAcknowledgesIncrement(context.getPlayerId(), 5L);
        context.setCommand(new Command(context.getPlayer(), context.getTable().getTableId(),
                context.getTable().getGameId(), "uuid", Command.CommandType.Ack.getCode()));
        context.playerIsPlaying();

        final boolean result = context.preprocess(underTest);

        assertThat(result, is(false));
        final Long acknowledgedIncrement = context.getTable().playerAtTable(
                context.getPlayer().getId()).getAcknowledgedIncrement();
        assertThat(acknowledgedIncrement, is(equalTo(5L)));
    }

}
