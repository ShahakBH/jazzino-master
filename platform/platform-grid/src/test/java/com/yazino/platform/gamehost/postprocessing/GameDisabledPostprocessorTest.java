package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertTrue;

public class GameDisabledPostprocessorTest {
    private GameDisabledPostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new GameDisabledPostprocessor(context.getGameRepository(), context.getDestinationFactory());
    }

    @Test
    public void if_game_is_not_disabled_and_game_not_finished_no_processing_occurs() throws GameException {
        context.getTable().setTableStatus(TableStatus.closed);
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void if_game_is_not_disabled_and_game_finished_no_processing_occurs() throws GameException {
        context.getTable().setTableStatus(TableStatus.closed);
        context.setGameEnabled(true);
        context.newGameCanBeClosed();
        context.postprocess(underTest);
        context.verifyGameAvailablityIsChecked();
        context.verifyNoHostInteractions();
    }

    @Test
    public void if_game_is_disabled_but_game_not_finished_no_processing_occurrs() throws GameException {
        context.getTable().setTableStatus(TableStatus.closing);
        context.setGameEnabled(true);
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void if_game_is_disabled_and_game_is_finished_players_are_not_transferred_and_document_is_sent_to_all_players_and_halt_processing_is_returned() throws GameException {
        context.setGameEnabled(false);
        context.newGameCanBeClosed();
        context.playerIsPlaying();
        context.postprocess(underTest);
        assertTrue("Message 'This game has been disabled' is expected", context.getTablePlayerDocument().getBody().contains("This game has been disabled"));
    }

    @Test
    public void if_game_is_disabled_post_process_for_null_command_is_handled() throws GameException {
        context.setGameEnabled(false);
        context.setCommand(null);
        context.newGameCanBeClosed();
        context.playerIsPlaying();
        context.postprocess(underTest);
        assertTrue("Message 'This game has been disabled' is expected", context.getTablePlayerDocument().getBody().contains("This game has been disabled"));
    }
}
