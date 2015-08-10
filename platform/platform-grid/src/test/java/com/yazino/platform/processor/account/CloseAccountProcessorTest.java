package com.yazino.platform.processor.account;

import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.CloseAccountRequest;
import com.yazino.platform.repository.account.AccountRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class CloseAccountProcessorTest {
    private AccountRepository accountRepository;
    private CloseAccountProcessor underTest;

    @Before
    public void init() {
        underTest = new CloseAccountProcessor();
        accountRepository = mock(AccountRepository.class);
        underTest.setAccountRepository(accountRepository);
    }

    @Test
    public void processRequestClosesAccount() {
        final Account account = mock(Account.class);
        final BigDecimal accountId = BigDecimal.ONE;

        when(accountRepository.findById(accountId)).thenReturn(account);
        when(accountRepository.lock(accountId)).thenReturn(account);

        account.setOpen(false);

        underTest.process(new CloseAccountRequest(accountId));

        verify(accountRepository).save(account);
    }

    @Test
    public void processRequestExitsCleanlyOnMissingObject() {
        final BigDecimal accountId = BigDecimal.ONE;

        when(accountRepository.findById(accountId)).thenReturn(null);

        underTest.process(new CloseAccountRequest(accountId));
    }
}
