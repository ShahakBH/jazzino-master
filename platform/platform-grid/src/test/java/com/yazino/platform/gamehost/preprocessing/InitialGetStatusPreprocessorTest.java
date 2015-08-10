package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.processor.table.ProcessingTestContext;
import com.yazino.platform.service.audit.Auditor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameException;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class InitialGetStatusPreprocessorTest {
    @Mock
    private Auditor auditor;

    private InitialGetStatusPreprocessor underTest;
    private ProcessingTestContext context;

    @Before
    public void init() {
        context = new ProcessingTestContext();
        underTest = new InitialGetStatusPreprocessor(auditor, context.getDestinationFactory());
    }

    @Test
    public void when_command_is_getstatus_getstatus_document_isreturned_and_and_processing_halt_result_returned() throws GameException {
        context.setGetStatusCommand();
        boolean result = context.preprocess(underTest);
        Assert.assertEquals(DocumentType.INITIAL_GAME_STATUS.getName(), context.getSinglePlayerDocument().getType());
        assertFalse(result);
    }

    @Test
    public void when_command_is_not_getstatus_processsing_continue_result_is_returned() throws GameException {
        boolean result = context.preprocess(underTest);
        context.verifyNoHostInteractions();
        assertTrue(result);
    }

    @Test
    public void when_stus_is_null_continue_result_is_returned() throws GameException {
        context.setGetStatusCommand();
        context.setCommand(new Command(context.getPlayer(), BigDecimal.TEN, 1L, "uuid", Command.CommandType.InitialGetStatus.getCode()));
        context.getTable().setCurrentGame(null);
        boolean result = context.preprocess(underTest);
        context.verifyNoHostInteractions();
        assertTrue(result);
    }
}
