package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.processor.table.ProcessingTestContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameException;
import com.yazino.game.api.GameStatus;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ObserverNotificationPostprocessorTest {
    private ObserverNotificationPostprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new ObserverNotificationPostprocessor(context.getGameRepository(), context.getDestinationFactory());
    }

    @Test
    public void when_result_is_null_no_docment_is_sent() throws GameException {
        context.setExecutionResult(null);
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void when_result_has_no_status_no_document_is_sent() throws GameException {
        GameStatus gameStatus = mock(GameStatus.class);
        context.setExecutionResult(new ExecutionResult.Builder(context.getGameRules(), gameStatus).build());
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void when_status_has_not_changed_no_document_is_sent() throws GameException {
        context.setExecutionResult(new ExecutionResult.Builder(context.getGameRules(), context.getGameStatus()).build());
        context.postprocess(underTest);
        context.verifyNoHostInteractions();
    }

    @Test
    public void when_status_has_changed_document_is_sent() throws GameException {
        context.postprocess(underTest);
        Assert.assertEquals(DocumentType.GAME_STATUS.name(), context.getObserverDocument().getType());
    }
}
