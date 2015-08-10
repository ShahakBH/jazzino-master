package com.yazino.platform.service.account;

import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.CloseAccountRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.account.AccountRepository;
import com.yazino.platform.service.account.transactional.TransactionalWalletService;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.persistence.account.AccountLoadType.NULL_IF_ABSENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceInternalWalletServiceTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(1);
    private static final String ACCOUNT_NAME = "aNewAccount";
    private static final String REFERENCE = "aReference";
    private static final String AUDIT_LABEL = "anAuditLabel";
    private static final String TX_TYPE = "stake";
    private static final BigDecimal CREDIT_LIMIT = BigDecimal.TEN;
    private static final BigDecimal STARTING_BALANCE = BigDecimal.valueOf(100);
    private static final long GAME_ID = 123l;
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(666);

    @Mock
    private GigaSpace globalGigaSpace;
    @Mock
    private AccountRepository accountGlobalRepository;
    @Mock
    private AccountRepository injectedAccountRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private TransactionalWalletService injectTransactionalWalletService;

    private GigaspaceInternalWalletService underTest;

    @Before
    public void setUp() {
        final Map<String, Object> injectedServices = newHashMap();
        injectedServices.put("accountRepository", injectedAccountRepository);
        injectedServices.put("transactionalWalletService", injectTransactionalWalletService);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(3, injectedServices, ACCOUNT_ID);

        underTest = new GigaspaceInternalWalletService(globalGigaSpace, accountGlobalRepository, sequenceGenerator, executor);
    }

    @Test(expected = NullPointerException.class)
    public void aNullAccountIdCannotBeClosed() {
        underTest.closeAccount(null);
    }

    @Test
    public void accountClosureWritesAClosureRequestToTheSpace() {
        underTest.closeAccount(ACCOUNT_ID);

        verify(globalGigaSpace).write(new CloseAccountRequest(ACCOUNT_ID));
    }

    @Test
    public void postTransactionDelegatesToTheTransactionalService() throws WalletServiceException {
        when(injectTransactionalWalletService.postTransaction(
                ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext()))
                .thenReturn(BigDecimal.TEN);

        underTest.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext());

        verify(injectTransactionalWalletService).postTransaction(
                ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext());
    }

    @Test(expected = WalletServiceException.class)
    public void postTransactionEnsuresExceptionsAreNotWrapped() throws WalletServiceException {
        when(injectTransactionalWalletService.postTransaction(
                ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext()))
                .thenThrow(new WalletServiceException(new ParameterisedMessage("Insufficient balance"), false));

        underTest.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext());
    }

    @Test
    public void accountCreationSavesANewAccountToTheRepository() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(ACCOUNT_ID);

        underTest.createAccount(ACCOUNT_NAME);

        verify(injectedAccountRepository).save(new Account(ACCOUNT_ID, ACCOUNT_NAME, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    public void accountCreationReturnsTheNewAccountId() throws WalletServiceException {
        when(sequenceGenerator.next()).thenReturn(ACCOUNT_ID);

        final BigDecimal newAccountId = underTest.createAccount(ACCOUNT_NAME);

        assertThat(newAccountId, is(equalTo(ACCOUNT_ID)));
    }

    @Test(expected = RuntimeException.class)
    public void aFailedAccountCreationPropagatesTheException() throws WalletServiceException {
        when(sequenceGenerator.next()).thenThrow(
                new RuntimeException("anException"));

        underTest.createAccount(ACCOUNT_NAME);
    }

    @Test
    public void balanceQueriesForDistributedMultipleAccountsReturnBalancesForAll() throws WalletServiceException {
        when(injectedAccountRepository.findById(bd(1), NULL_IF_ABSENT))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(anAccountWithIdAndBalance(1, 10));
        when(injectedAccountRepository.findById(bd(2), NULL_IF_ABSENT))
                .thenReturn(null)
                .thenReturn(anAccountWithIdAndBalance(2, 20))
                .thenReturn(null);
        when(injectedAccountRepository.findById(bd(3), NULL_IF_ABSENT))
                .thenReturn(null)
                .thenReturn(anAccountWithIdAndBalance(3, 30))
                .thenReturn(null);

        final Map<BigDecimal, BigDecimal> balances = underTest.getBalances(newHashSet(bd(1), bd(2), bd(3)));

        assertThat(balances.size(), is(equalTo(3)));
        assertThat(balances.get(bd(1)), is(equalTo(bd(10))));
        assertThat(balances.get(bd(2)), is(equalTo(bd(20))));
        assertThat(balances.get(bd(3)), is(equalTo(bd(30))));
    }

    @Test
    public void balanceQueriesForMultipleAccountsReturnBalancesForAll() throws WalletServiceException {
        when(injectedAccountRepository.findById(bd(1), NULL_IF_ABSENT)).thenReturn(anAccountWithIdAndBalance(1, 10));
        when(injectedAccountRepository.findById(bd(2), NULL_IF_ABSENT)).thenReturn(anAccountWithIdAndBalance(2, 20));

        final Map<BigDecimal, BigDecimal> balances = underTest.getBalances(newHashSet(bd(1), bd(2)));

        assertThat(balances.size(), is(equalTo(2)));
        assertThat(balances.get(bd(1)), is(equalTo(bd(10))));
        assertThat(balances.get(bd(2)), is(equalTo(bd(20))));
    }

    @Test
    public void balanceQueriesForMultipleAccountsIgnoreBalancesForNullAccounts() throws WalletServiceException {
        when(injectedAccountRepository.findById(bd(3), NULL_IF_ABSENT)).thenReturn(anAccountWithIdAndBalance(3, 30));

        final Map<BigDecimal, BigDecimal> balances = underTest.getBalances(newHashSet(bd(1), bd(3)));

        assertThat(balances.size(), is(equalTo(1)));
        assertThat(balances.get(bd(3)), is(equalTo(bd(30))));
    }

    @Test
    public void balanceQueriesForAnEmptySetOfMultipleAccountsReturnAnEmptyMap() throws WalletServiceException {
        final Map<BigDecimal, BigDecimal> balances = underTest.getBalances(Collections.<BigDecimal>emptySet());

        assertThat(balances.size(), is(equalTo(0)));
    }

    @Test
    public void balanceQueriesForMultipleAccountsSkipAnyMissingResults() throws WalletServiceException {
        when(injectedAccountRepository.findById(bd(1), NULL_IF_ABSENT))
                .thenThrow(new RuntimeException("anException"))
                .thenReturn(null)
                .thenReturn(null);
        when(injectedAccountRepository.findById(bd(2), NULL_IF_ABSENT)).thenReturn(anAccountWithIdAndBalance(2, 10));

        final Map<BigDecimal, BigDecimal> balances = underTest.getBalances(newHashSet(bd(1), bd(2)));

        assertThat(balances.size(), is(equalTo(1)));
        assertThat(balances.get(bd(2)), is(equalTo(bd(10))));
    }

    @Test(expected = NullPointerException.class)
    public void balanceQueriesForNullAccountsAreRejected() throws WalletServiceException {
        underTest.getBalance(null);
    }

    @Test
    public void balanceQueriesForAnAccountShouldReturnTheAccountBalancePlusCreditLimit()
            throws WalletServiceException {
        final Account account = anAccountWithBalance(STARTING_BALANCE);
        account.setCreditLimit(CREDIT_LIMIT);
        when(accountGlobalRepository.findById(ACCOUNT_ID)).thenReturn(account);

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        Assert.assertThat(accountBalance, CoreMatchers.is(CoreMatchers.equalTo(STARTING_BALANCE.add(CREDIT_LIMIT))));
    }

    @Test
    public void balanceQueriesForAnAccountShouldReturnTheAccountBalanceWhereTheCreditLimitIsZero()
            throws WalletServiceException {
        when(accountGlobalRepository.findById(ACCOUNT_ID)).thenReturn(anAccountWithBalance(STARTING_BALANCE));

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        Assert.assertThat(accountBalance, CoreMatchers.is(CoreMatchers.equalTo(STARTING_BALANCE)));
    }


    @Test
    public void balanceQueriesForAnAccountShouldReturnTheAccountBalanceWhereTheCreditLimitIsNull()
            throws WalletServiceException {
        final Account account = anAccountWithBalance(STARTING_BALANCE);
        account.setCreditLimit(null);
        when(accountGlobalRepository.findById(ACCOUNT_ID)).thenReturn(account);

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        Assert.assertThat(accountBalance, CoreMatchers.is(CoreMatchers.equalTo(STARTING_BALANCE)));
    }

    @Test
    public void balanceQueriesForAnAccountShouldReturnZeroIfTheBalanceIsNull()
            throws WalletServiceException {
        when(accountGlobalRepository.findById(ACCOUNT_ID)).thenReturn(anAccountWithBalance(null));

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        Assert.assertThat(accountBalance, CoreMatchers.is(CoreMatchers.equalTo(BigDecimal.ZERO)));
    }

    @Test(expected = WalletServiceException.class)
    public void balanceQueriesForANonExistentAccountShouldThrowAWalletServiceException()
            throws WalletServiceException {
        when(accountGlobalRepository.findById(ACCOUNT_ID)).thenReturn(null);

        underTest.getBalance(ACCOUNT_ID);
    }

    private Account anAccountWithIdAndBalance(final long id,
                                              final long balance) {
        return new Account(bd(id), ACCOUNT_NAME, bd(balance));
    }

    private Account anAccountWithBalance(final BigDecimal balance) {
        return new Account(ACCOUNT_ID, "account1", balance);
    }

    private BigDecimal bd(final long value) {
        return BigDecimal.valueOf(value);
    }

    private TransactionContext aContext() {
        return transactionContext().withGameId(GAME_ID).withTableId(TABLE_ID).withSessionId(BigDecimal.valueOf(3141592)).build();
    }
}
