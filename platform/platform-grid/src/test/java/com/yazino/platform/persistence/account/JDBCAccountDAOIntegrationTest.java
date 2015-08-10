package com.yazino.platform.persistence.account;

import com.yazino.platform.model.account.Account;
import com.yazino.platform.persistence.SequenceGenerator;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class JDBCAccountDAOIntegrationTest {
    private static final String accountName = "xyz12345";
    private static final String PARTNER_ID = "INTERNAL";

    @Autowired
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    private AccountDAO accountDAO;
    private AtomicLong seq = new AtomicLong(-1000000L);

    @Before
    public void setUp() {
        accountDAO = new JDBCAccountDAO(jdbc);
    }

    private BigDecimal getBalance(BigDecimal accountId) {
        return jdbc.queryForObject("select balance from ACCOUNT where ACCOUNT_ID=?",
                new Object[]{accountId}, BigDecimal.class);
    }

    @Transactional
    @Before
    public void init() {
        setUpAccountForTest(accountName, BigDecimal.ZERO);
    }

    private long setUpAccountForTest(final String accountName,
                                     final BigDecimal initialBalance) {
        final BigDecimal accountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID, NAME, BALANCE) values (?,?,?)", accountId, accountName, initialBalance);
        final long playerId = seq.decrementAndGet();
        jdbc.execute(String.format("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (%d,'YAZINO')", playerId));
        jdbc.update("insert into PLAYER(PLAYER_ID,ACCOUNT_ID) values (?,?)", playerId, accountId);
        return accountId.longValue();
    }

    @Transactional
    @Test
    public void test_createAccountCreatesAccount() {
        String userIdNew = accountName + "new";
        BigDecimal accId = createAccount(userIdNew, BigDecimal.ZERO);
        String fromDb = jdbc.queryForObject("select NAME from ACCOUNT where ACCOUNT_ID=?", String.class, accId);
        Assert.assertEquals(userIdNew, fromDb);
    }

    @Transactional
    @Test
    public void test_saveAccountChangesBalance() {
        BigDecimal accId = createAccount(PARTNER_ID + accountName, BigDecimal.ZERO);
        final BigDecimal amount = BigDecimal.TEN;
        accountDAO.saveAccount(new Account(accId, "name", amount));
        Assert.assertEquals(0, BigDecimal.TEN.compareTo(getBalance(accId)));
    }

    @Transactional
    @Test
    public void findByIdShouldReturnMatchingObject() {
        final String accountName = PARTNER_ID + JDBCAccountDAOIntegrationTest.accountName + "1";
        final BigDecimal accountId = createAccount(accountName, BigDecimal.ZERO);

        final Account account = accountDAO.findById(accountId);

        assertThat(account.getAccountId(), is(comparesEqualTo(accountId)));
        assertThat(account.getName(), is(equalTo(accountName)));
    }

    @Transactional
    @Test
    public void findByIdShouldReturnNullIfNoMatchingObjectExists() {
        final Account account = accountDAO.findById(new BigDecimal("99999999999"));
        assertThat(account, is(nullValue()));
    }

    private BigDecimal createAccount(final String name,
                                     final BigDecimal creditLimit) {
        final Account account = new Account(sequenceGenerator.next(), name, BigDecimal.ZERO, creditLimit);
        accountDAO.saveAccount(account);
        return account.getAccountId();
    }

}
