package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GameException;

public class TableAuditPostprocessorTest {
    private TableAuditPostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new TableAuditPostprocessor(context.getAuditor(), context.getGameCompletePublisher(), context.getGameRepository());
    }

    @Test
    public void when_game_is_complete_table_auditor_is_used() throws GameException {
        context.gameIsFinished();
        context.postprocess(underTest);
        context.verifyTableAudited();
    }

    @Test
    public void when_game_is_complete_game_complete_message_is_sent() throws GameException {
        context.gameIsFinished();
        context.postprocess(underTest);
        context.verifyGameCompleteSent();
    }

}
