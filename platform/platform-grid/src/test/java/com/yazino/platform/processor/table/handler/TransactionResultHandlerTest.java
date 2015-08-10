package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TransactionResultWrapper;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.service.audit.CommonsLoggingAuditor;
import com.yazino.platform.table.TableStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameType;
import com.yazino.game.api.TransactionResult;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionResultHandlerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(104L);
    private static final long GAME_ID = 1542L;

    @Mock
    private GameHost gameHost;
    @Mock
    private TableRepository tableRepository;
    @Mock
    private GameRepository gameRepository;

    private TransactionResultHandler underTest;
    private Table table;
    private TransactionResult transactionResult;

    @Before
    public void setUp() {
        table = new Table(new GameType("TEST", "Test", Collections.<String>emptySet()), BigDecimal.ONE, null, false);
        table.setTableId(TABLE_ID);
        table.setTableStatus(TableStatus.open);
        table.setVariationProperties(new HashMap<String, String>());
        transactionResult = new TransactionResult("foo", true, null, null, null, null);

        underTest = new TransactionResultHandler();
        underTest.setTableRepository(tableRepository);
        underTest.setGameRepository(gameRepository);
        underTest.setGameHost(gameHost);
        underTest.setAuditor(new CommonsLoggingAuditor());
    }

    @Test
    public void testResultPassedToExecute() {
        when(tableRepository.findById(eq(TABLE_ID))).thenReturn(table);

        when(gameHost.processTransactionResult(eq(table), eq(GAME_ID), eq(transactionResult)))
                .thenReturn(Collections.<HostDocument>emptyList());

        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                TABLE_ID, GAME_ID, transactionResult, null);
        underTest.handle(wrappedResult);

        verify(gameHost).removeAllPlayers(table);
        verify(tableRepository).save(table);
    }

    @Test
    public void testExecutionFailureSetTableCacheStatusToError() {
        when(tableRepository.findById(eq(TABLE_ID))).thenReturn(table);

        when(gameHost.processTransactionResult(eq(table), eq(GAME_ID), eq(transactionResult)))
                .thenThrow(new RuntimeException("Err14234"));

        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                TABLE_ID, GAME_ID, transactionResult, null);
        underTest.handle(wrappedResult);

        assertEquals(TableStatus.error, table.getTableStatus());
        assertTrue(table.getMonitoringMessage().contains("Err14234"));
        verify(gameHost).removeAllPlayers(table);
        verify(tableRepository).save(eq(table));
    }

    @Test
    public void testTableInErrorIsIgnored() {
        when(tableRepository.findById(eq(TABLE_ID))).thenReturn(table);

        table.setTableStatus(TableStatus.error);

        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                TABLE_ID, GAME_ID, transactionResult, null);
        underTest.handle(wrappedResult);
    }

    @Test(expected = NullPointerException.class)
    public void testNullParameterShouldThrowNullPointerException() {
        underTest.handle(null);
    }

    @Test
    public void testNullTableIdShouldReturnQuietly() {
        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                null, GAME_ID, transactionResult, null);
        underTest.handle(wrappedResult);
    }

    @Test
    public void testNullGameIdShouldReturnQuietly() {
        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                TABLE_ID, null, transactionResult, null);
        underTest.handle(wrappedResult);
    }

    @Test
    public void testNullTransactionResultShouldReturnQuietly() {
        final TransactionResultWrapper wrappedResult = new TransactionResultWrapper(
                TABLE_ID, GAME_ID, null, null);
        underTest.handle(wrappedResult);
    }
}
