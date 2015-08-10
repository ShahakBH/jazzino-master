package com.yazino.platform.model.account;

import com.yazino.platform.error.account.InsufficientFundsException;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class AccountTest {

    private static final long CURRENT_TIME = 45443534545343L;
    private static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;
    private static final BigDecimal AMOUNT = new BigDecimal(11);
    private static final String TYPE = "stake";
    private static final String REFERENCE = "ref";
    private static final BigDecimal BALANCE = BigDecimal.TEN;
    private static final AccountTransaction TRANSACTION = new AccountTransaction(ACCOUNT_ID, AMOUNT, TYPE, REFERENCE);
    private static final AccountTransaction POPULATED_TRANSACTION = new AccountTransaction(
            TRANSACTION, CURRENT_TIME, BALANCE.add(AMOUNT));

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_TIME);
    }

    @After
    public void resetDate() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void postTransaction_updates_balance() throws InsufficientFundsException {
        final Account account = account();

        account.postTransaction(TRANSACTION);

        Assert.assertThat(account.getBalance(), is(equalTo(BALANCE.add(AMOUNT))));
    }

    @Test
    public void transactionAreGivenATimestampAndRunningBalance() throws InsufficientFundsException {
        final Account account = account();

        account.postTransaction(TRANSACTION);

        Assert.assertThat(account.popNewAccountTransactions(), Matchers.hasItem(POPULATED_TRANSACTION));
    }

    @Test
    public void shouldKeepPostedUntilPopped() throws Exception {
        Account account = account();

        account.postTransaction(TRANSACTION);

        Assert.assertEquals(Arrays.asList(POPULATED_TRANSACTION), account.popNewAccountTransactions());
        Assert.assertEquals(0, account.popNewAccountTransactions().size());
    }

    @Test
    public void postTransactionRecognisesCreditLimit() throws InsufficientFundsException {
        final BigDecimal creditLimit = AMOUNT.add(BigDecimal.ONE);
        final BigDecimal withdrawal = BigDecimal.ZERO.subtract(AMOUNT);

        Account account = new Account(ACCOUNT_ID, "test", BigDecimal.ZERO, creditLimit);
        account.postTransaction(new AccountTransaction(ACCOUNT_ID, withdrawal, TYPE, REFERENCE));
        Assert.assertEquals(withdrawal, account.getBalance());
        Assert.assertEquals(creditLimit, account.getCreditLimit());
    }

    @Test
    public void postTransaction_refuses_foreign_transactions() throws InsufficientFundsException {
        Account a = account();
        try {
            a.postTransaction(new AccountTransaction(ACCOUNT_ID.add(BigDecimal.ONE), AMOUNT, TYPE, REFERENCE));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(BALANCE, a.getBalance());
        }
    }

    @Test(expected = InsufficientFundsException.class)
    public void postTransactionRejectsIfBalanceInsufficient() throws InsufficientFundsException {
        final Account account = account();
        final BigDecimal withdrawal = BigDecimal.ZERO.subtract(BALANCE.add(BigDecimal.ONE));

        account.postTransaction(new AccountTransaction(ACCOUNT_ID, withdrawal, TYPE, REFERENCE));
    }

    @Test(expected = InsufficientFundsException.class)
    public void postTransactionRejectsIfAvailableBalanceAndCreditLimitInsufficient() throws InsufficientFundsException {
        final BigDecimal creditLimit = BigDecimal.TEN;
        final Account account = new Account(ACCOUNT_ID, "test", BALANCE, creditLimit);
        final BigDecimal withdrawal = BigDecimal.ZERO.subtract(BigDecimal.ONE.add(BALANCE).add(creditLimit));

        account.postTransaction(new AccountTransaction(ACCOUNT_ID, withdrawal, TYPE, REFERENCE));
    }

    @Test
    public void createAccount_close_it_than_reopen_ti() throws InsufficientFundsException {
        Account a = account();
        a.setOpen(false);
        Assert.assertFalse(a.isOpen());

        a.setOpen(true);
        Assert.assertTrue(a.isOpen());
    }

    private Account account() {
        return new Account(ACCOUNT_ID, "test", BALANCE);
    }
}
