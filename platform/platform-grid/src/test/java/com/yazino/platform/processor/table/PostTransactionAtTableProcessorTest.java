package com.yazino.platform.processor.table;

import com.yazino.game.api.TransactionResult;
import com.yazino.game.api.TransactionType;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.table.PostTransactionAtTable;
import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.model.table.TableTransactionRequest;
import com.yazino.platform.model.table.TransactionResultWrapper;
import com.yazino.platform.service.account.InternalWalletService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PostTransactionAtTableProcessorTest {

    private final GigaSpace gigaSpace = mock(GigaSpace.class);
    private final InternalWalletService internalWalletService = mock(InternalWalletService.class);
    private final PostTransactionAtTableProcessor underTest = new PostTransactionAtTableProcessor(gigaSpace, internalWalletService);

    private final BigDecimal accountId1 = BigDecimal.valueOf(2);
    private final BigDecimal playerId1 = BigDecimal.valueOf(1);
    private final BigDecimal playerId2 = BigDecimal.valueOf(5);
    private final BigDecimal accountId2 = BigDecimal.valueOf(6);

    private final BigDecimal releaseRequest1 = BigDecimal.valueOf(7);
    private final BigDecimal releaseRequest2 = BigDecimal.valueOf(8);
    private final String auditLabel = "FooBar";

    private PostTransactionAtTable transaction1;
    private PostTransactionAtTable transaction2;

    private TableTransactionRequest request;


    @Before
    public void init() {
        String transactionType = TransactionType.Return.name();
        String reference = "reference";
        BigDecimal tableId = BigDecimal.valueOf(4);
        long gameId = 5;

        final TransactionContext transactionContext = transactionContext().withGameId(gameId).withTableId(tableId).withSessionId(BigDecimal.ONE).build();
        transaction1 = new PostTransactionAtTable(playerId1, accountId1, BigDecimal.valueOf(3), transactionType, reference, "uid1", transactionContext);
        transaction2 = new PostTransactionAtTable(playerId2, accountId2, BigDecimal.valueOf(7), transactionType, reference, "uid2", transactionContext);

        Set<BigDecimal> releaseRequests = new HashSet<BigDecimal>(Arrays.asList(releaseRequest1, releaseRequest2));
        List<PostTransactionAtTable> transfers = Arrays.asList(transaction1, transaction2);
        request = new TableTransactionRequest(tableId, transfers, releaseRequests, auditLabel);
    }

    @Test
    public void should_process_all_transactions() throws WalletServiceException {
        underTest.processTransactionRequest(request);
        verifyTransfer(transaction1);
        verifyTransfer(transaction2);
    }

    @Test
    public void should_write_successful_transaction_responses_to_gigaspaces() throws WalletServiceException {
        final BigDecimal newBalance1 = BigDecimal.ONE;
        final BigDecimal newBalance2 = BigDecimal.TEN;
        when(internalWalletService.postTransaction(eq(transaction1.getAccountId()), any(BigDecimal.class), any(String.class), any(String.class), any(TransactionContext.class))).thenReturn(newBalance1);
        when(internalWalletService.postTransaction(eq(transaction2.getAccountId()), any(BigDecimal.class), any(String.class), any(String.class), any(TransactionContext.class))).thenReturn(newBalance2);

        underTest.processTransactionRequest(request);

        final TransactionResult transactionResult1 = new TransactionResult(transaction1.getUniqueId(), true, null, transaction1.getAccountId(), newBalance1, playerId1);
        final TransactionResult transactionResult2 = new TransactionResult(transaction2.getUniqueId(), true, null, transaction2.getAccountId(), newBalance2, playerId2);

        final ArgumentCaptor<TableRequestWrapper> requestWrapper = ArgumentCaptor.forClass(TableRequestWrapper.class);
        verify(gigaSpace, times(2)).write(requestWrapper.capture());

        final List<TableRequestWrapper> values = requestWrapper.getAllValues();

        assertThat(values.get(0).getTableId(), is(equalTo(request.getTableId())));
        assertThat(((TransactionResultWrapper) values.get(0).getTableRequest()).getGameId(), is(equalTo(transaction1.getTransactionContext().getGameId())));
        assertThat(((TransactionResultWrapper) values.get(0).getTableRequest()).getTableId(), is(equalTo(transaction1.getTransactionContext().getTableId())));
        assertThat(((TransactionResultWrapper) values.get(0).getTableRequest()).getTransactionResult(), is(equalTo(transactionResult1)));

        assertThat(values.get(1).getTableId(), is(equalTo(request.getTableId())));
        assertThat(((TransactionResultWrapper) values.get(1).getTableRequest()).getGameId(), is(equalTo(transaction2.getTransactionContext().getGameId())));
        assertThat(((TransactionResultWrapper) values.get(1).getTableRequest()).getTableId(), is(equalTo(transaction2.getTransactionContext().getTableId())));
        assertThat(((TransactionResultWrapper) values.get(1).getTableRequest()).getTransactionResult(), is(equalTo(transactionResult2)));
    }

    @Test
    public void should_write_failed_transaction_responses_to_gigaspaces() throws WalletServiceException {
        final BigDecimal newBalance = BigDecimal.TEN;
        when(internalWalletService.getBalance(accountId1)).thenReturn(newBalance);
        doThrow(new RuntimeException("message")).when(internalWalletService).postTransaction(
                any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString(), any(TransactionContext.class));

        underTest.processTransactionRequest(request);

        final TransactionResult transactionResult1 = new TransactionResult(transaction1.getUniqueId(), false, "message", transaction1.getAccountId(), null, playerId1);
        final TransactionResult transactionResult2 = new TransactionResult(transaction2.getUniqueId(), false, "message", transaction2.getAccountId(), null, playerId2);

        final ArgumentCaptor<TableRequestWrapper> requestWrapper = ArgumentCaptor.forClass(TableRequestWrapper.class);
        verify(gigaSpace, times(2)).write(requestWrapper.capture());

        final List<TableRequestWrapper> values = requestWrapper.getAllValues();

        assertThat(values.get(0).getTableId(), is(equalTo(request.getTableId())));
        assertThat(((TransactionResultWrapper) values.get(0).getTableRequest()).getGameId(), is(equalTo(transaction1.getTransactionContext().getGameId())));
        assertThat((values.get(0).getTableRequest()).getTableId(), is(equalTo(transaction1.getTransactionContext().getTableId())));
        assertThat(((TransactionResultWrapper) values.get(0).getTableRequest()).getTransactionResult(), is(equalTo(transactionResult1)));

        assertThat(values.get(1).getTableId(), is(equalTo(request.getTableId())));
        assertThat(((TransactionResultWrapper) values.get(1).getTableRequest()).getGameId(), is(equalTo(transaction2.getTransactionContext().getGameId())));
        assertThat((values.get(1).getTableRequest()).getTableId(), is(equalTo(transaction2.getTransactionContext().getTableId())));
        assertThat(((TransactionResultWrapper) values.get(1).getTableRequest()).getTransactionResult(), is(equalTo(transactionResult2)));
    }

    private void verifyTransfer(PostTransactionAtTable transfer) throws WalletServiceException {
        verify(internalWalletService).postTransaction(transfer.getAccountId(), transfer.getAmount(), transfer.getTransactionType(),
                transfer.getTransactionReference(), transfer.getTransactionContext());
    }

}
