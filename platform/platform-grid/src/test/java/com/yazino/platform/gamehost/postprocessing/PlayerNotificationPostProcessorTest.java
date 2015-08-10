package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

import static org.junit.Assert.assertEquals;

public class PlayerNotificationPostProcessorTest {
    private PlayerNotificationPostProcessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new PlayerNotificationPostProcessor(context.getDestinationFactory(), context.getGameRepository());
    }

    @Test
    public void when_there_are_no_players_nothing_is_sent() throws GameException {
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void status_documents_dispatched_to_player_and_status_request_is_sent() throws GameException {
        context.playerIsPlaying();
        context.newStatusHasChanges();
        context.newGameIsFinished();
        context.postprocess(underTest);

        assertEquals(DocumentType.GAME_STATUS.name(), context.getSinglePlayerDocument().getType());
    }
}
