package com.yazino.host.table.wallet;

import com.yazino.host.TableRequestWrapperQueue;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.table.PlayerInformation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.yazino.game.api.TransactionResult;
import com.yazino.game.api.TransactionType;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.TransactionResultWrapper;

import java.math.BigDecimal;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static java.math.BigDecimal.valueOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class StandaloneBufferedGameHostWalletTest {

    private static final BigDecimal ACCOUNT_ID = valueOf(123);
    private static final BigDecimal TABLE_ID = BigDecimal.ONE;
    private static final long GAME_ID = 1l;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.TEN;
    private PlayerInformation playerInformation;

    private TableRequestWrapperQueue requestQueue;
    private WalletService walletService;

    private StandaloneBufferedGameHostWallet underTest;

    @Before
    public void setUp() {
        requestQueue = mock(TableRequestWrapperQueue.class);
        walletService = mock(WalletService.class);
        underTest = new StandaloneBufferedGameHostWallet(walletService, requestQueue);
        playerInformation = new PlayerInformation(PLAYER_ID, "aPlayer", ACCOUNT_ID, SESSION_ID, valueOf(123));
    }

    @Test
    public void shouldHoldTransactionExecutionUntilFlush() throws WalletServiceException {
        underTest.post(TABLE_ID,
                GAME_ID,
                playerInformation,
                valueOf(200),
                TransactionType.Stake,
                "audit",
                "reference",
                "reply");
        assertEquals(1, underTest.numberOfPendingTransactions());
        verifyZeroInteractions(walletService, requestQueue);
    }

    @Test
    public void shouldGetBalanceFromWallet() throws WalletServiceException {
        when(walletService.getBalance(ACCOUNT_ID)).thenReturn(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, underTest.getBalance(ACCOUNT_ID));
    }

    @Test
    public void shouldProcessPendingTransactionAndAddResultToQueueForSuccessfulTx() throws WalletServiceException {
        final BigDecimal updatedBalance = valueOf(546);
        when(walletService.postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class)))
                .thenReturn(updatedBalance);
        underTest.post(TABLE_ID,
                GAME_ID,
                playerInformation,
                valueOf(200),
                TransactionType.Stake,
                "audit",
                "reference",
                "reply");
        underTest.flush();
        verify(walletService).postTransaction(ACCOUNT_ID,
                valueOf(200),
                TransactionType.Stake.name(),
                "reference",
                aTransactionContext());
        final TransactionResult result = new TransactionResult("reply",
                true,
                null,
                ACCOUNT_ID,
                updatedBalance,
                PLAYER_ID);
        final TransactionResultWrapper resultWrapper = new TransactionResultWrapper(TABLE_ID, GAME_ID, result, "reply");
        verifyRequestAddedToQueue(resultWrapper);
    }

    private TransactionContext aTransactionContext() {
        return transactionContext()
                .withGameId(GAME_ID)
                .withTableId(TABLE_ID)
                .withSessionId(SESSION_ID)
                .withPlayerId(PLAYER_ID)
                .build();
    }

    @Test
    public void shouldProcessPendingTransactionAndAddResultToQueueForUnsuccessfulTx() throws WalletServiceException {
        when(walletService.postTransaction(any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class)))
                .thenThrow(new WalletServiceException("no chips"));
        underTest.post(TABLE_ID,
                GAME_ID,
                playerInformation,
                valueOf(200),
                TransactionType.Stake,
                "audit",
                "reference",
                "reply");
        underTest.flush();
        verify(walletService).postTransaction(ACCOUNT_ID,
                valueOf(200),
                TransactionType.Stake.name(),
                "reference",
                aTransactionContext());
        final TransactionResult result = new TransactionResult("reply",
                false,
                "no chips",
                ACCOUNT_ID,
                null,
                PLAYER_ID);
        final TransactionResultWrapper resultWrapper = new TransactionResultWrapper(TABLE_ID, GAME_ID, result, "reply");
        verifyRequestAddedToQueue(resultWrapper);
    }

    private void verifyRequestAddedToQueue(TransactionResultWrapper wrapper) {
        ArgumentCaptor<TableRequestWrapper> requestCaptor = ArgumentCaptor.forClass(TableRequestWrapper.class);
        verify(requestQueue).addRequest(requestCaptor.capture());
        final TableRequestWrapper actual = requestCaptor.getValue();
        final TableRequestWrapper expected = new TableRequestWrapper(wrapper);
        expected.setRequestID(actual.getRequestID());
        assertEquals(expected, actual);
    }

}
