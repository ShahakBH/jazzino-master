package com.yazino.platform.processor.account;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.Transaction;
import com.yazino.platform.model.account.AccountTransaction;
import com.yazino.platform.model.account.AccountTransactionPersistenceRequest;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AccountTransactionPersistenceProcessorTest {

    private final AuditService auditService = mock(AuditService.class);

    private final AccountTransactionPersistenceProcessor underTest = new AccountTransactionPersistenceProcessor();

    private final AccountTransaction accountTransaction = new AccountTransaction(BigDecimal.ONE, BigDecimal.TEN, "foo", "bar");
    private final AccountTransactionPersistenceRequest request = new AccountTransactionPersistenceRequest(accountTransaction);

    @Before
    public void setUp() {
        underTest.setAccountDAO(auditService);
    }

    @Test
    public void aSingleTransactionIsPersistedAndReturnsNull() {
        final AccountTransactionPersistenceRequest[] result = underTest.processAccountPersistenceRequest(requests(request));

        assertThat(result, is(nullValue()));
        verify(auditService).transactionsProcessed(asList(message(accountTransaction)));
    }

    @Test
    public void multipleTransactionsArePersistedAndReturnNull() {
        final AccountTransaction accountTransaction2 = new AccountTransaction(BigDecimal.ONE, BigDecimal.TEN, "foo", "bar");
        final AccountTransactionPersistenceRequest request2 = new AccountTransactionPersistenceRequest(accountTransaction2);

        final AccountTransactionPersistenceRequest[] result = underTest.processAccountPersistenceRequest(requests(request, request2));

        assertThat(result, is(nullValue()));
        verify(auditService).transactionsProcessed(asList(message(accountTransaction), message(accountTransaction2)));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void aFailedSaveForASingleTransactionReturnsATheRequestWithErrorStatus() {
        doThrow(new RuntimeException("anException")).when(auditService).transactionsProcessed(asList(message(accountTransaction)));

        final AccountTransactionPersistenceRequest[] result = underTest.processAccountPersistenceRequest(requests(request));

        assertThat(result, is(not(nullValue())));
        assertThat(result.length, is(equalTo(1)));
        assertThat(errorState(request), is(equalTo(result[0])));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void aFailedSaveForMultipleTransactionsReturnsTheRequestsWithErrorStatus() {
        final AccountTransaction accountTransaction2 = new AccountTransaction(BigDecimal.ONE, BigDecimal.TEN, "foo", "bar");
        final AccountTransactionPersistenceRequest request2 = new AccountTransactionPersistenceRequest(accountTransaction2);

        doThrow(new RuntimeException("anException")).when(auditService).transactionsProcessed(asList(message(accountTransaction), message(accountTransaction2)));

        final AccountTransactionPersistenceRequest[] result = underTest.processAccountPersistenceRequest(requests(request, request2));

        assertThat(result, is(not(nullValue())));
        assertThat(result.length, is(equalTo(2)));
        assertThat(errorState(request), is(equalTo(result[0])));
        assertThat(errorState(request2), is(equalTo(result[1])));
    }

    private AccountTransactionPersistenceRequest errorState(final AccountTransactionPersistenceRequest request) {
        request.setStatus(AccountTransactionPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private AccountTransactionPersistenceRequest[] requests(final AccountTransactionPersistenceRequest... requests) {
        return requests;
    }

    private Transaction message(AccountTransaction tx) {
        return new Transaction(tx.getAccountId(), tx.getAmount(), tx.getType(), tx.getReference(), tx.getTimestamp(), tx.getRunningBalance(),
                tx.getTransactionContext().getGameId(), tx.getTransactionContext().getTableId(),
                tx.getTransactionContext().getSessionId(), tx.getTransactionContext().getPlayerId());
    }


}
