package com.yazino.platform.service.account;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.platform.model.account.Account;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.platform.model.account.ExternalTransactionPersistenceRequest;
import com.yazino.platform.persistence.account.JDBCAccountStatementDAO;
import com.yazino.platform.repository.account.AccountRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.transactional.TransactionalWalletService;
import com.yazino.platform.service.community.CashierInformation;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.platform.account.ExternalTransactionStatus.CANCELLED;
import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.model.account.AccountStatement.AccountStatementBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingWalletServiceTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(1);
    private static final String REFERENCE = "aReference";
    private static final String TX_TYPE = "stake";
    private static final BigDecimal STARTING_BALANCE = BigDecimal.valueOf(100);
    private static final BigDecimal EXTERNAL_TX_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal CREDIT_LIMIT = BigDecimal.TEN;
    private static final Long GAME_ID = 123l;
    private static final BigDecimal tableId = BigDecimal.valueOf(666);
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(100);

    @Mock
    private AccountRepository localAccountRepository;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private TransactionalWalletService transactionalWalletService;
    @Mock
    private InternalWalletService internalWalletService;

    private GigaspaceRemotingWalletService underTest;
    private Account account;
    @Mock
    private JDBCAccountStatementDAO accountStatementDao;
    @Mock
    private CashierInformation cashierInformation;

    @Mock
    private PlayerRepository playerRepository;

    @Before
    public void setUp() throws Exception {
        underTest = new GigaspaceRemotingWalletService(
                localAccountRepository, gigaSpace, transactionalWalletService, accountStatementDao, internalWalletService, cashierInformation, playerRepository);

        account = anAccountWithBalance(STARTING_BALANCE);

        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(account);
        when(localAccountRepository.lock(ACCOUNT_ID)).thenReturn(account);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void postTransactionDelegatesToTheTransactionalService() throws WalletServiceException {
        when(transactionalWalletService.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext()))
                .thenReturn(BigDecimal.TEN);

        underTest.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext());

        verify(transactionalWalletService).postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, aContext());
    }

    @Test
    public void postTransactionWithoutAnAuditLabelDelegatesToTheTransactionalService() throws WalletServiceException {
        when(transactionalWalletService.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, TransactionContext.EMPTY))
                .thenReturn(BigDecimal.TEN);

        underTest.postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, TransactionContext.EMPTY);

        verify(transactionalWalletService).postTransaction(ACCOUNT_ID, BigDecimal.valueOf(15), TX_TYPE, REFERENCE, TransactionContext.EMPTY);
    }

    @Test
    public void postingAnExternalTransactionResponseUpdatesTheAccount()
            throws InsufficientFundsException, WalletServiceException {
        when(transactionalWalletService.postTransaction(ACCOUNT_ID, EXTERNAL_TX_AMOUNT, "Wirecard Deposit", "1I",
                transactionContext().withSessionId(SESSION_ID).build()))
                .thenAnswer(new Answer<BigDecimal>() {
                    @Override
                    public BigDecimal answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        account.setBalance(account.getBalance().add((BigDecimal) invocationOnMock.getArguments()[1]));
                        return account.getBalance();
                    }
                });

        underTest.record(anExternalTransaction());

        assertThat(account.getBalance(), is(equalTo(STARTING_BALANCE.add(EXTERNAL_TX_AMOUNT))));
    }

    @Test
    public void postingAnExternalTransactionResponseWritesAPersistenceRequestToTheSpace()
            throws InsufficientFundsException, WalletServiceException {
        underTest.record(anExternalTransaction());

        verify(gigaSpace).write(new ExternalTransactionPersistenceRequest(anExternalTransaction()));
    }

    @Test
    public void loggingAFailingExternalTransactionResponseWritesAPersistenceRequestToTheSpaceAndDoesNotPost()
            throws InsufficientFundsException, WalletServiceException {
        underTest.record(anExternalTransaction(FAILURE));

        verify(gigaSpace).write(new ExternalTransactionPersistenceRequest(anExternalTransaction(FAILURE)));
        verifyNoMoreInteractions(transactionalWalletService);
    }

    @Test
    public void loggingACancelledExternalTransactionResponseWritesAPersistenceRequestToTheSpaceAndDoesNotPost()
            throws InsufficientFundsException, WalletServiceException {
        underTest.record(anExternalTransaction(CANCELLED));

        verify(gigaSpace).write(new ExternalTransactionPersistenceRequest(anExternalTransaction(CANCELLED)));
        verifyNoMoreInteractions(transactionalWalletService);
    }

    @Test(expected = NullPointerException.class)
    public void postingANullExternalTransactionResponseThrowsAnException()
            throws InsufficientFundsException, WalletServiceException {
        underTest.record(null);
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
        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(account);

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        assertThat(accountBalance, is(equalTo(STARTING_BALANCE.add(CREDIT_LIMIT))));
    }

    @Test
    public void balanceQueriesForAnAccountShouldReturnTheAccountBalanceWhereTheCreditLimitIsZero()
            throws WalletServiceException {
        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(anAccountWithBalance(STARTING_BALANCE));

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        assertThat(accountBalance, is(equalTo(STARTING_BALANCE)));
    }


    @Test
    public void balanceQueriesForAnAccountShouldReturnTheAccountBalanceWhereTheCreditLimitIsNull()
            throws WalletServiceException {
        final Account account = anAccountWithBalance(STARTING_BALANCE);
        account.setCreditLimit(null);
        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(account);

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        assertThat(accountBalance, is(equalTo(STARTING_BALANCE)));
    }

    @Test
    public void balanceQueriesForAnAccountShouldReturnZeroIfTheBalanceIsNull()
            throws WalletServiceException {
        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(anAccountWithBalance(null));

        final BigDecimal accountBalance = underTest.getBalance(ACCOUNT_ID);

        assertThat(accountBalance, is(equalTo(BigDecimal.ZERO)));
    }

    @Test(expected = WalletServiceException.class)
    public void balanceQueriesForANonExistentAccountShouldThrowAWalletServiceException()
            throws WalletServiceException {
        when(localAccountRepository.findById(ACCOUNT_ID)).thenReturn(null);

        underTest.getBalance(ACCOUNT_ID);
    }

    @Test
    public void getValueOfTodaysEarnedChipsShouldReturnAggregatedValue() {
        final String cashier = "facebookEarnChips";
        final BigDecimal accountId = BigDecimal.ONE;
        final List<AccountStatement> accountStatements = newArrayList();
        accountStatements.add(new AccountStatementBuilder(BigDecimal.ONE).withChipsAmount(new BigDecimal(5)).asStatement());
        accountStatements.add(new AccountStatementBuilder(BigDecimal.ONE).withChipsAmount(new BigDecimal(2)).asStatement());
        accountStatements.add(new AccountStatementBuilder(BigDecimal.ONE).withChipsAmount(new BigDecimal(3)).asStatement());
        when(accountStatementDao.findBy(accountId, cashier)).thenReturn(accountStatements);
        assertThat(underTest.getValueOfTodaysEarnedChips(accountId, cashier), is(equalTo(new BigDecimal(10))));

    }

    private TransactionContext aContext() {
        return transactionContext().withGameId(GAME_ID).withTableId(tableId).withSessionId(BigDecimal.valueOf(3141592)).build();
    }

    private ExternalTransaction anExternalTransaction() {
        return anExternalTransaction(ExternalTransactionStatus.SUCCESS);
    }

    private ExternalTransaction anExternalTransaction(ExternalTransactionStatus status) {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("1I")
                .withExternalTransactionId("1E")
                .withMessage("<xml>Bla bla</xml>", new DateTime())
                .withAmount(Currency.getInstance("USD"), new BigDecimal("10"))
                .withPaymentOption(EXTERNAL_TX_AMOUNT, null)
                .withCreditCardNumber("4200XXXXXXXX0000")
                .withCashierName("Wirecard")
                .withStatus(status)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("ROULETTE")
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(123l)
                .withPlatform(Platform.WEB)
                .build();
    }


    private Account anAccountWithBalance(final BigDecimal balance) {
        return new Account(ACCOUNT_ID, "account1", balance);
    }
}
