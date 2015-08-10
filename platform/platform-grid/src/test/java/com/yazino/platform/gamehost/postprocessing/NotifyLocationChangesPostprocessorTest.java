package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameException;
import com.yazino.game.api.GameStatus;

import static org.mockito.Mockito.mock;

public class NotifyLocationChangesPostprocessorTest {
    private NotifyLocationChangesPostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new NotifyLocationChangesPostprocessor(context.getGameRepository(),
                context.getPlayerRepository(), context.getChatRepository(), context.getLocationService());
        context.setGameEnabled(true);
    }

    @Test
    public void when_there_is_no_result_no_processing_occurrs() throws GameException {
        context.setExecutionResult(null);
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void when_there_is_no_new_status_all_players_are_leaving_location() throws GameException {
        GameStatus gameStatus = mock(GameStatus.class);
        context.setExecutionResult(new ExecutionResult.Builder(context.getGameRules(), gameStatus).build());
        context.playerIsPlaying();
        context.postprocess(underTest);
        context.verifyPlayerLeavesLocation();
    }

    @Test
    public void when_is_not_playing_anymore_player_is_leaving_location() throws GameException {
        context.playerIsLeaving();
        context.postprocess(underTest);
        context.verifyPlayerLeavesLocation();
    }


    @Test
    public void when_player_is_still_no_messages_are_sent() throws GameException {
        context.playerIsPlaying();
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void when_table_is_closed_all_players_are_leaving() throws GameException {
        context.playerIsPlaying();
        context.getTable().setTableStatus(TableStatus.closed);
        context.postprocess(underTest);
        context.verifyPlayerLeavesLocation();
    }

    @Test
    public void when_table_is_closing_and_game_is_complete_all_players_are_leaving() throws GameException {
        context.playerIsPlaying();
        context.gameCanBeClosed();
        context.getTable().setTableStatus(TableStatus.closing);
        context.postprocess(underTest);
        context.verifyPlayerLeavesLocation();
    }

    @Test
    public void when_game_is_disabled_and_game_is_complete_all_players_are_leaving() throws GameException {
        context.playerIsPlaying();
        context.setGameEnabled(false);
        context.newGameCanBeClosed();
        context.postprocess(underTest);
        context.verifyPlayerLeavesLocation();
    }

    @Test
    public void whenThereIsNoResultNoChangesAreMadeToThePlayerCache() throws GameException {
        context.setExecutionResult(null);
        context.postprocess(underTest);
        context.verifyPlayerRemainsInCache();
    }
}
