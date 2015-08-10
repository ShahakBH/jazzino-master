package strata.server.worker.audit.persistence;

import com.yazino.platform.Platform;
import com.yazino.platform.audit.message.ExternalTransaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAnAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional("externalDwTransactionManager")
@DirtiesContext
public class PostgresExternalTransactionDAOIntegrationTest {
    private static final String EXTERNAL_TX_ID = "anExternalTransactionForJDBCExternalTransactionDAOTest";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(666);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresExternalTransactionDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM EXTERNAL_TRANSACTION WHERE EXTERNAL_TRANSACTION_ID=?", EXTERNAL_TX_ID);
        createAnAccount(jdbc, ACCOUNT_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void saveCannotBeCalledWhenClassWasInitialisedViaTheCGLibConstructor() {
        new PostgresExternalTransactionDAO().save(newHashSet(anExternalTransaction(ACCOUNT_ID)));
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresExternalTransactionDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(anExternalTransaction(ACCOUNT_ID)));
    }

    @Test(expected = CannotAcquireLockException.class)
    public void transientProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresExternalTransactionDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotAcquireLockException("aTestException", new SQLException()));

        underTest.saveAll(asList(anExternalTransaction(ACCOUNT_ID)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresExternalTransactionDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(anExternalTransaction(ACCOUNT_ID)));
    }

    @Test
    public void aPlayerIDCanBeFoundByInternalTransactionID() {
        underTest.saveAll(asList(anExternalTransaction(ACCOUNT_ID)));

        final BigDecimal playerId = underTest.findPlayerIdFor("anInternalTxId");

        assertThat(playerId, is(equalTo(PLAYER_ID)));
    }

    @Test
    public void aNonExistentTransactionReturnsNullWhenFindingPlayerID() {
        final BigDecimal playerId = underTest.findPlayerIdFor("aNonExistentInternalTxId");

        assertThat(playerId, is(nullValue()));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void nullExternalTransactionsThrowANullPointerException() {
        underTest.saveAll(null);
    }

    @Test
    public void anExternalTransactionIsSavedToTheDatabase() {
        underTest.saveAll(asList(anExternalTransaction(ACCOUNT_ID)));

        verifyMatches(anExternalTransaction(ACCOUNT_ID), readRecord(EXTERNAL_TX_ID));
    }

    @Test
    public void anExternalTransactionWithALargeExternalIdIsSavedToTheDatabase() {
        final ExternalTransaction externalTransaction = anExternalTransaction(ACCOUNT_ID);
        final String externalTransactionId = "2:2OT8D0cu9aHWpzF0Ic5RuAB66ZuMYE52A3pkuFBAeY9KGie2vQ4KBTTtJhw5yAHW6GnxvGoxx68SpTqkm7jDztmDKU5DQgv5O2UvBF4s8nzshYtq9LF1ajJJI6BHwxvIkiz7-ofhH1l9tU939zmbX_xyv8ZOxLSOMMfA3P0_xCOFYkZbVo1LaQYSk16rE1tis3ycJ3A0j9oc756pAJAbLMEHTgvUSJowbrQfSW4AVqgCHKvFqT6jM2J0RNTYdz0QxOjo-wwAvxAUtdihOKEucamtssEebYzoEYxxutOfmSfUF7A6r-lZ3KrTjuoEUVO-MslQYpi_N1LeUwhhZekmMQ==:KQvU4n1MHP1vAQke_yaUVQ==";
        externalTransaction.setExternalTransactionId(externalTransactionId);
        underTest.saveAll(asList(externalTransaction));

        verifyMatches(externalTransaction, readRecord(externalTransactionId));
    }

    @Test
    public void multipleExternalTransactionsAreSavedToTheDatabase() {
        underTest.saveAll(asList(anExternalTransaction(BigDecimal.valueOf(-1)),
                anExternalTransaction(BigDecimal.valueOf(-2)),
                anExternalTransaction(BigDecimal.valueOf(-3))));

        verifyMatches(anExternalTransaction(BigDecimal.valueOf(-1)), readRecordByAccountId(BigDecimal.valueOf(-1)));
        verifyMatches(anExternalTransaction(BigDecimal.valueOf(-2)), readRecordByAccountId(BigDecimal.valueOf(-2)));
        verifyMatches(anExternalTransaction(BigDecimal.valueOf(-3)), readRecordByAccountId(BigDecimal.valueOf(-3)));
    }

    private void verifyMatches(final ExternalTransaction externalTransaction, final Map<String, Object> record) {
        assertThat((BigDecimal) record.get("ACCOUNT_ID"), is(comparesEqualTo(externalTransaction.getAccountId())));
        assertThat(record.get("EXTERNAL_TRANSACTION_ID").toString(), is(equalTo(externalTransaction.getExternalTransactionId())));
        assertThat(record.get("INTERNAL_TRANSACTION_ID").toString(), is(equalTo(externalTransaction.getInternalTransactionId())));
        assertThat(record.get("MESSAGE").toString(), is(equalTo(externalTransaction.getCreditCardObscuredMessage())));
        assertThat((Timestamp) record.get("MESSAGE_TS"), is(equalTo(new Timestamp(externalTransaction.getMessageTimeStamp().getTime()))));
        assertThat(record.get("CURRENCY_CODE").toString(), is(equalTo(externalTransaction.getCurrency())));
        assertThat(((BigDecimal) record.get("AMOUNT")).setScale(2), is(equalTo(externalTransaction.getAmountCash().setScale(2))));
        assertThat(record.get("CREDIT_CARD_NUMBER").toString(), is(equalTo(externalTransaction.getObscuredCreditCardNumber())));
        assertThat(record.get("EXTERNAL_TRANSACTION_STATUS").toString(), is(equalTo(externalTransaction.getExternalTransactionStatus())));
        assertThat(((BigDecimal) record.get("AMOUNT_CHIPS")).setScale(2), is(equalTo(externalTransaction.getAmountChips().setScale(2))));
        assertThat(record.get("TRANSACTION_TYPE").toString(), is(equalTo(externalTransaction.getTransactionLogType())));
        assertThat(record.get("GAME_TYPE").toString(), is(equalTo(externalTransaction.getGameType())));
        assertThat(record.get("PAYMENT_OPTION_ID").toString(), is(equalTo(externalTransaction.getPaymentOptionId())));
        assertThat(record.get("BASE_CURRENCY_CODE").toString(), is(equalTo(externalTransaction.getBaseCurrency())));
        assertThat(((BigDecimal) record.get("BASE_CURRENCY_AMOUNT")).setScale(2), is(equalTo(externalTransaction.getBaseCurrencyAmount().setScale(2))));
        assertThat(((BigDecimal) record.get("EXCHANGE_RATE")).setScale(6), is(equalTo(externalTransaction.getExchangeRate().setScale(6))));
        assertThat((String) record.get("FAILURE_REASON"), is(equalTo(externalTransaction.getFailureReason())));
    }

    private Map<String, Object> readRecord(final String externalTxId) {
        return jdbc.queryForMap("SELECT * FROM EXTERNAL_TRANSACTION WHERE EXTERNAL_TRANSACTION_ID=?", externalTxId);
    }

    private Map<String, Object> readRecordByAccountId(final BigDecimal accountId) {
        return jdbc.queryForMap("SELECT * FROM EXTERNAL_TRANSACTION WHERE ACCOUNT_ID=?", accountId);
    }

    private ExternalTransaction anExternalTransaction(final BigDecimal accountId) {
        return new ExternalTransaction(accountId, "anInternalTxId", EXTERNAL_TX_ID, "anObscuredMessage",
                new Date(1000000), "GBP", BigDecimal.valueOf(200), BigDecimal.valueOf(3000), "aCCNumber", "aCashier",
                "aGameType", "anExternalStatus", "aLogType", PLAYER_ID, 123l, Platform.WEB,
                "aPaymentOptionId", "USD", BigDecimal.valueOf(300), new BigDecimal("0.59234"), "aFailureReason");
    }

}
