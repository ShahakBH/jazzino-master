package com.yazino.platform.persistence.account;

import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class JDBCAccountStatementDAOIntegrationTest {
    private static final String TEST_PREFIX = "jdbcAccountStatementTest-";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JDBCAccountStatementDAO underTest;

    private BigDecimal accountId;

    @BeforeClass
    public static void lockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed((System.currentTimeMillis() / 1000) * 1000);//truncates the milliseconds
    }

    @AfterClass
    public static void unlockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Before
    public void setUpAccount() {
        final BigDecimal minAccountId = jdbc.queryForObject(
                "SELECT IFNULL(MIN(ACCOUNT_ID),0) from ACCOUNT where ACCOUNT_ID < 0", BigDecimal.class);
        accountId = minAccountId.subtract(BigDecimal.ONE);
        jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) VALUES (?,?)", accountId, "jdbcAccountStatement");
    }

    @After
    public void cleanup() {
        jdbc.execute("DELETE FROM ACCOUNT_STATEMENT WHERE INTERNAL_TRANSACTION_ID LIKE '" + TEST_PREFIX + "%'");
        if (accountId != null) {
            jdbc.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID = ?", accountId);
            accountId = null;
        }
    }

    @Test
    @Transactional
    public void aFullyPopulatedAccountStatementCanBeSaved() {
        final AccountStatement statement = anAccountStatement(0);

        underTest.save(statement);

        final Map<String, Object> objectMap = readStatementToMap(0);
        assertThat(objectMap.get("INTERNAL_TRANSACTION_ID").toString(),
                is(equalTo(statement.getInternalTransactionId())));
        assertThat(objectMap.get("ACCOUNT_ID").toString(), is(equalTo(statement.getAccountId().toString())));
        assertThat(objectMap.get("CASHIER_NAME").toString(), is(equalTo(statement.getCashierName())));
        assertThat(objectMap.get("GAME_TYPE").toString(), is(equalTo(statement.getGameType())));
        assertThat(objectMap.get("TRANSACTION_STATUS").toString(),
                is(equalTo(statement.getTransactionStatus().toString())));
        assertThat(asDateTime(objectMap.get("PURCHASE_TIMESTAMP")), is(equalTo(statement.getTimestamp())));
        assertThat(objectMap.get("PURCHASE_CURRENCY").toString(),
                is(equalTo(statement.getPurchaseCurrency().getCurrencyCode())));
        assertThat((BigDecimal) objectMap.get("PURCHASE_AMOUNT"), is(equalTo(statement.getPurchaseAmount().setScale(4))));
        assertThat((BigDecimal) objectMap.get("CHIPS_AMOUNT"), is(equalTo(statement.getChipsAmount().setScale(4))));
    }

    @Test
    @Transactional
    public void aMinimallyPopulatedAccountStatementCanBeSaved() {
        final AccountStatement statement = aMinimalAccountStatement(0);

        underTest.save(statement);

        final Map<String, Object> objectMap = readStatementToMap(0);
        assertThat(objectMap.get("INTERNAL_TRANSACTION_ID").toString(),
                is(equalTo(statement.getInternalTransactionId())));
        assertThat(objectMap.get("ACCOUNT_ID").toString(), is(equalTo(statement.getAccountId().toString())));
        assertThat(objectMap.get("CASHIER_NAME").toString(), is(equalTo(statement.getCashierName())));
        assertThat(asDateTime(objectMap.get("PURCHASE_TIMESTAMP")), is(equalTo(statement.getTimestamp())));
        assertThat(objectMap.get("PURCHASE_CURRENCY").toString(),
                is(equalTo(statement.getPurchaseCurrency().getCurrencyCode())));
        assertThat((BigDecimal) objectMap.get("PURCHASE_AMOUNT"), is(equalTo(statement.getPurchaseAmount().setScale(4))));
        assertThat((BigDecimal) objectMap.get("CHIPS_AMOUNT"), is(equalTo(statement.getChipsAmount().setScale(4))));

        assertThat(objectMap.get("GAME_TYPE"), is(nullValue()));
        assertThat(objectMap.get("TRANSACTION_STATUS"), is(nullValue()));
    }

    @Test
    @Transactional
    public void aTransactionCanBeRetrievedByInternalTransactionid() {
        final AccountStatement desiredStatement = anAccountStatement(1);
        underTest.save(anAccountStatement(0));
        underTest.save(desiredStatement);
        underTest.save(anAccountStatement(2));

        final AccountStatement foundStatement = underTest.findByInternalTransactionId(internalTransactionIdFor(1));

        assertThat(foundStatement, is(equalTo(desiredStatement)));
    }

    @Test
    @Transactional
    public void transactionsCanBeRetrievedByDateAndCashier() {
        final AccountStatement statement1 = anAccountStatementOn(1, new DateTime(), "testCashier");
        final AccountStatement statement2 = anAccountStatementOn(2, new DateTime(), "testCashier");

        underTest.save(anAccountStatementOn(0, new DateTime().minusDays(1), "testCashier"));
        underTest.save(statement1);
        underTest.save(statement2);

        final List<AccountStatement> foundStatements = underTest.findBy(accountId, "testCashier");

        assertTrue(foundStatements.contains(statement1));
        assertTrue(foundStatements.contains(statement2));
    }

    private DateTime asDateTime(final Object date) {
        if (date != null && date instanceof Date) {
            return new DateTime(((Date) date).getTime());
        }
        return null;
    }

    private Map<String, Object> readStatementToMap(final int internalTransactionIdSuffix) {
        return jdbc.queryForMap("SELECT * FROM ACCOUNT_STATEMENT WHERE INTERNAL_TRANSACTION_ID=?",
                internalTransactionIdFor(internalTransactionIdSuffix));
    }

    public AccountStatement anAccountStatement(final int internalTransactionIdSuffix) {
        return anAccountStatementOn(internalTransactionIdSuffix, new DateTime(), "testCashier");
    }

    private AccountStatement anAccountStatementOn(final int internalTransactionIdSuffix, DateTime theTimestamp, String cashier) {
        return AccountStatement.forAccount(accountId)
                .withInternalTransactionId(internalTransactionIdFor(internalTransactionIdSuffix))
                .withTimestamp(theTimestamp)
                .withGameType("BLACKJACK")
                .withCashierName(cashier)
                .withTransactionStatus(ExternalTransactionStatus.SUCCESS)
                .withPurchaseCurrency(Currency.getInstance("GBP"))
                .withPurchaseAmount(BigDecimal.valueOf(100).setScale(4))
                .withChipsAmount(BigDecimal.valueOf(20000).setScale(4))
                .asStatement();
    }

    public AccountStatement aMinimalAccountStatement(final int internalTransactionIdSuffix) {
        return AccountStatement.forAccount(accountId)
                .withInternalTransactionId(internalTransactionIdFor(internalTransactionIdSuffix))
                .withTimestamp(new DateTime())
                .withCashierName("testCashier")
                .withPurchaseCurrency(Currency.getInstance("GBP"))
                .withPurchaseAmount(BigDecimal.valueOf(100))
                .withChipsAmount(BigDecimal.valueOf(20000))
                .asStatement();
    }

    private String internalTransactionIdFor(final int internalTransactionIdSuffix) {
        return TEST_PREFIX + internalTransactionIdSuffix;
    }

}
