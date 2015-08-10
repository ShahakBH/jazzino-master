package strata.server.worker.audit.persistence;

import com.yazino.platform.audit.message.Transaction;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import utils.PostgresTestValueHelper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class PostgresTransactionLogDAOIntegrationTest {
    private static final BigDecimal PLAYER_ID = new BigDecimal("234");
    private static final BigDecimal ACCOUNT_ID = new BigDecimal("10.01");
    private static final BigDecimal SESSION_ID = new BigDecimal("-3141592.00");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresTransactionLogDAO transactionLogDAO;

    @Before
    public void setUp() throws Exception {
        cleanUpTestDataFor(ACCOUNT_ID, SESSION_ID);
        PostgresTestValueHelper.createAnAccount(jdbc, ACCOUNT_ID);
        jdbc.update("INSERT INTO ACCOUNT_SESSION (ACCOUNT_ID,SESSION_KEY,SESSION_ID) VALUES (?,?,?)", ACCOUNT_ID, getClass().getName(), SESSION_ID);
    }

    @After
    public void cleanUpTransactions() {
        cleanUpTestDataFor(ACCOUNT_ID, SESSION_ID);
    }

    @Test
    public void multipleTransactionAreAddedToTheDatabase() {
        final Transaction tx1 = tx(ACCOUNT_ID, "10.0000", "Topup", "Test transaction", now(), null, new BigDecimal(123), new BigDecimal("100.12"), SESSION_ID, PLAYER_ID);
        final Transaction tx2 = tx(ACCOUNT_ID, "15.0000", "Level Bonus", "Test transaction", now(), "2434.2500", null, null, null, PLAYER_ID);
        final Transaction tx3 = tx(ACCOUNT_ID, "-10.0000", "Stake", "Test transaction", now(), "1000.0000", new BigDecimal(666), new BigDecimal("1.00"), SESSION_ID, PLAYER_ID);
        transactionLogDAO.saveAll(asList(tx1, tx2, tx3));

        final List<Transaction> accountTransactions = getTransactionsForAccount(ACCOUNT_ID);

        assertThat(3, is(equalTo(accountTransactions.size())));
        assertThat(accountTransactions.get(0), is(equalTo(tx2)));
        assertThat(accountTransactions.get(1), is(equalTo(tx1)));
        assertThat(accountTransactions.get(2), is(equalTo(tx3)));
    }

    @Test
    public void anEscapedTransactionIsAddedToTheDatabase() {
        final Transaction tx1 = tx(ACCOUNT_ID, "10.0000", "Topup", "Test transaction with t'Escape", now(), null, new BigDecimal(123), new BigDecimal("100.12"), SESSION_ID, PLAYER_ID);
        transactionLogDAO.saveAll(asList(tx1));

        final List<Transaction> accountTransactions = getTransactionsForAccount(ACCOUNT_ID);

        assertThat(1, is(equalTo(accountTransactions.size())));
        assertThat(accountTransactions.get(0), is(equalTo(tx1)));
    }

    @Test
    public void aTransactionListWithASingleTransactionIsAddedToTheDatabase() {
        final Transaction tx1 = tx(ACCOUNT_ID, "10.00", "Topup", "Test transaction", now(), null, new BigDecimal(123), new BigDecimal("100.12"), SESSION_ID, PLAYER_ID);

        transactionLogDAO.saveAll(asList(tx1));

        final List<Transaction> loadedTransactions = getTransactionsForAccount(ACCOUNT_ID);

        assertThat(loadedTransactions.size(), is(equalTo(1)));
        assertThat(loadedTransactions.get(0), is(equalTo(tx1)));
    }

    @Test
    public void aTransactionListWithNoElementsIsIgnored() {
        transactionLogDAO.saveAll(Collections.<Transaction>emptyList());

        final List<Transaction> accountTransactions = getTransactionsForAccount(ACCOUNT_ID);

        assertThat(accountTransactions.size(), is(equalTo(0)));
    }

    @Test
    public void supportsMillisecondPrecisionForTransactionTimestamps() {
        assertTrue(getTransactionsForAccount(ACCOUNT_ID).isEmpty());
        final long timestampWithNonZeroMillis = new DateTime(2011, 12, 31, 23, 59, 59, 3).getMillis();
        final Transaction sample = tx(ACCOUNT_ID, "10.00", "Topup", "Test transaction", timestampWithNonZeroMillis, null,
                new BigDecimal(234), BigDecimal.valueOf(123), BigDecimal.valueOf(-3141592L), PLAYER_ID);
        transactionLogDAO.saveAll(asList(sample));

        final List<Transaction> accountTransactions = getTransactionsForAccount(ACCOUNT_ID);
        assertThat(accountTransactions.size(), is(equalTo(1)));
        assertThat(accountTransactions.get(0).getTimestamp(), is(equalTo(timestampWithNonZeroMillis)));
    }

    private Long toTimestamp(final Timestamp timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Null timestamp");
        }
        return timestamp.getTime();
    }

    private Long now() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime().getTime();
    }

    private Transaction tx(final BigDecimal accountId,
                           final String s,
                           final String stake,
                           final String s1,
                           final Long now,
                           final String s2,
                           BigDecimal gameId,
                           BigDecimal tableId,
                           BigDecimal sessionId,
                           final BigDecimal playerId) {
        return new Transaction(accountId, new BigDecimal(s), stake, s1, now, s2 != null ? new BigDecimal(s2) : null,
                gameId == null ? 0 : gameId.longValue(), tableId, sessionId, playerId);
    }

    private void cleanUpTestDataFor(final BigDecimal accountId,
                                    final BigDecimal sessionId) {
        jdbc.update("DELETE FROM TRANSACTION_LOG WHERE ACCOUNT_ID=?", accountId);
        jdbc.update("DELETE FROM ACCOUNT_SESSION WHERE SESSION_ID=?", sessionId);
    }

    @SuppressWarnings({"unchecked"})
    private List<Transaction> getTransactionsForAccount(final BigDecimal accountId) {
        return jdbc.query("SELECT * FROM TRANSACTION_LOG WHERE ACCOUNT_ID=? ORDER BY amount DESC",
                new RowMapper() {
                    @Override
                    public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new Transaction(rs.getBigDecimal("ACCOUNT_ID"), rs.getBigDecimal("AMOUNT"), rs
                                .getString("TRANSACTION_TYPE"), rs.getString("REFERENCE"), toTimestamp(
                                rs.getTimestamp("TRANSACTION_TS")), rs.getBigDecimal("RUNNING_BALANCE"),
                                rs.getLong("GAME_ID"), rs.getBigDecimal("TABLE_ID"), rs.getBigDecimal("SESSION_ID"),
                                PLAYER_ID);
                    }
                }, accountId);
    }

}
