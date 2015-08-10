package com.yazino.platform.processor.account;

import com.yazino.platform.event.message.AccountEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountPersistenceRequest;
import com.yazino.platform.persistence.account.AccountDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.repository.account.AccountRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class AccountPersisterTest {
    private AccountPersistenceRequest request;
    private BigDecimal accountId;
    private AccountRepository accountRepository;
    private Account account;
    private AccountDAO accountDAO;
    private QueuePublishingService<AccountEvent> accountEventService;

    public AccountPersister underTest;

    @Before
    public void setUp() {
        accountId = BigDecimal.ONE;
        request = new AccountPersistenceRequest(accountId);
        accountRepository = mock(AccountRepository.class);
        account = mock(Account.class);
        accountDAO = mock(AccountDAO.class);
        accountEventService = mock(QueuePublishingService.class);
        underTest = new AccountPersister(accountDAO, accountRepository, accountEventService);
    }

    @Test
    public void testNullReturnedAndMatchingRequestsAreRemovedIfNoAccountFound() {
        when(accountRepository.findById(accountId)).thenReturn(null);

        final PersistenceRequest<BigDecimal> result = underTest.persist(request);

        assertNull(result);
    }


    @Test
    public void testSucessPathSavesAccountBalanceRemovesMatchingRequestsRemovesClosedAccountAndReturnsNull() {
        when(accountRepository.findById(accountId)).thenReturn(account);
        when(account.isOpen()).thenReturn(false);
        when(account.getAccountId()).thenReturn(BigDecimal.ONE);
        when(account.getBalance()).thenReturn(BigDecimal.TEN);

        final PersistenceRequest<BigDecimal> result = underTest.persist(request);

        assertNull(result);
        verify(accountDAO).saveAccount(account);
        verify(accountRepository).remove(account);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testErrorOnSavingAccountReturnsRequestInErrorState() {
        when(accountRepository.findById(accountId)).thenReturn(account);
        doThrow(new RuntimeException("foo")).when(accountDAO).saveAccount(account);
        AccountPersistenceRequest expected = new AccountPersistenceRequest(accountId);
        expected.setStatus(AccountPersistenceRequest.Status.ERROR);

        final PersistenceRequest<BigDecimal> result = underTest.persist(request);

        assertEquals(expected, result);
    }

}
