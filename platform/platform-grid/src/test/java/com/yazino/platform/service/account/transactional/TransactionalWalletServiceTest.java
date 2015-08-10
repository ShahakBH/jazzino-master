package com.yazino.platform.service.account.transactional;

import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.account.AccountRepository;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionalWalletServiceTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(1);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(-11);
    private static final BigDecimal STARTING_BALANCE = BigDecimal.valueOf(100);
    private static final String REFERENCE = "aReference";
    private static final String AUDIT_LABEL = "anAuditLabel";
    private static final String TX_TYPE = "stake";
    private static final BigDecimal CREDIT_LIMIT = BigDecimal.TEN;
    private static final BigDecimal CHILD_ACCOUNT_ID = BigDecimal.valueOf(9999);
    private static final Long gameId = 123l;
    private static final BigDecimal tableId = BigDecimal.valueOf(666);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;

    private TransactionalWalletService underTest;
    private Account account;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new TransactionalWalletService(accountRepository, sequenceGenerator);

        account = anAccountWithBalance(STARTING_BALANCE);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(account);
        when(accountRepository.lock(ACCOUNT_ID)).thenReturn(account);

        when(sequenceGenerator.next()).thenReturn(CHILD_ACCOUNT_ID);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aSuccessfulPostReturnsAnUpdatedBalance()
            throws InsufficientFundsException, WalletServiceException {
        final BigDecimal amountToAdd = CREDIT_LIMIT;

        final BigDecimal newBalance = underTest.postTransaction(
                ACCOUNT_ID, amountToAdd, TX_TYPE, REFERENCE, aContext());

        assertThat(newBalance, is(equalTo(STARTING_BALANCE.add(amountToAdd))));
    }

    @Test
    public void aSuccessfulPostUpdatesTheAccountWithTheNewBalance()
            throws InsufficientFundsException, WalletServiceException {
        final BigDecimal amountToSubtract = CREDIT_LIMIT;

        underTest.postTransaction(
                ACCOUNT_ID, BigDecimal.ZERO.subtract(amountToSubtract), TX_TYPE, REFERENCE, aContext());

        assertThat(account.getBalance(), is(equalTo(STARTING_BALANCE.subtract(amountToSubtract))));
    }

    @Test
    public void aSuccessfulPostLocksAndSavesTheAccount()
            throws InsufficientFundsException, WalletServiceException {
        final BigDecimal amountToAdd = CREDIT_LIMIT;

        underTest.postTransaction(ACCOUNT_ID, amountToAdd, TX_TYPE, REFERENCE, aContext());

        verify(accountRepository).lock(ACCOUNT_ID);
        verify(accountRepository).save(anAccountWithBalance(STARTING_BALANCE.add(amountToAdd)));
    }

    @Test(expected = WalletServiceException.class)
    public void aPostThatFailsShouldThrowAWalletServiceException()
            throws InsufficientFundsException, WalletServiceException {
        final BigDecimal amountToSubtract = STARTING_BALANCE.add(CREDIT_LIMIT);

        underTest.postTransaction(
                ACCOUNT_ID, BigDecimal.ZERO.subtract(amountToSubtract), TX_TYPE, REFERENCE, aContext());
    }

    @Test
    public void insufficientBalanceFailsShouldBeMarkedAsExpected() {
        final BigDecimal amountToSubtract = STARTING_BALANCE.add(CREDIT_LIMIT);

        try {
            underTest.postTransaction(
                    ACCOUNT_ID, BigDecimal.ZERO.subtract(amountToSubtract), TX_TYPE, REFERENCE, aContext());
            fail("No exception thrown");
        } catch (WalletServiceException e) {
            assertThat(e.isUnexpected(), is(false));
        }
    }

    private Account anAccountWithBalance(final BigDecimal balance) {
        return new Account(ACCOUNT_ID, "account1", balance);
    }

    private TransactionContext aContext() {
        return transactionContext().withGameId(gameId).withTableId(tableId).withSessionId(SESSION_ID).build();
    }
}
