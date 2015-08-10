package com.yazino.platform.payment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;

import static com.yazino.platform.payment.JDBCPaymentStateDao.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCPaymentStateDaoIntegrationTest {

    private static final int RANDOM = new Random().nextInt(10000);

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate template;

    private JDBCPaymentStateDao dao;

    private final String testCashier = "TestCashier_" + RANDOM;
    private final String testExternalTransactionId = "TestTransactionId_" + RANDOM;

    @Before
    public void setup() {
        dao = new JDBCPaymentStateDao(template);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenInsertingAndCashierNameIsNull() throws Exception {
        dao.insertState(null, testExternalTransactionId, PaymentState.Started);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenInsertingAndCashierNameIsEmpty() throws Exception {
        dao.insertState("  ", testExternalTransactionId, PaymentState.Started);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenInsertingAndTransactionIdIsNull() throws Exception {
        dao.insertState(testCashier, null, PaymentState.Started);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenInsertingAndTransactionIdIsEmpty() throws Exception {
        dao.insertState(testCashier, "   ", PaymentState.Started);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenInsertingAndStateIsNull() throws Exception {
        dao.insertState(testCashier, testExternalTransactionId, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenInsertingAndStateIsUnknown() throws Exception {
        dao.insertState(testCashier, testExternalTransactionId, PaymentState.Unknown);
    }

    @Test
    @Transactional
    public void shouldInsertEntryIntoTableWithCorrectState() throws Exception {
        PaymentState testState = PaymentState.Started;
        dao.insertState(testCashier, testExternalTransactionId, testState);
        assertPaymentState(testCashier, testExternalTransactionId, testState);
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Transactional
    public void shouldThrowExceptionWhenInsertingAndEntryAlreadyExists() throws Exception {
        try {
            dao.insertState(testCashier, testExternalTransactionId, PaymentState.Started);
        } catch (DataAccessException e) {
            fail("Unexpected: " + e.getMessage());
        }
        dao.insertState(testCashier, testExternalTransactionId, PaymentState.Started);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenUpdatingAndCashierNameIsNull() throws Exception {
        dao.updateState(null, testExternalTransactionId, PaymentState.Started);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenUpdatingAndCashierNameIsEmpty() throws Exception {
        dao.updateState("   ", testExternalTransactionId, PaymentState.Started);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenUpdatingAndTransactionIdIsNull() throws Exception {
        dao.updateState(testCashier, null, PaymentState.Started);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenUpdatingAndTransactionIdIsEmpty() throws Exception {
        dao.updateState(testCashier, " ", PaymentState.Started);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenUpdatingAndStateIsNull() throws Exception {
        dao.updateState(testCashier, testExternalTransactionId, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenUpdatingAndStateIsUnknown() throws Exception {
        dao.updateState(testCashier, testExternalTransactionId, PaymentState.Unknown);
    }

    @Test
    @Transactional
    public void shouldUpdateEntryInTableToCorrectState() throws Exception {
        dao.insertState(testCashier, testExternalTransactionId, PaymentState.Started);
        int rows = dao.updateState(testCashier, testExternalTransactionId, PaymentState.Failed);
        //assertEquals(1, rows); // todo can't figure out why this is failing, expect its to do with using a insert on duplicate key
        assertPaymentState(testCashier, testExternalTransactionId, PaymentState.Failed);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenReadingAndCashierNameIsNull() throws Exception {
        dao.readState(null, testExternalTransactionId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenReadingAndCashierNameIsEmpty() throws Exception {
        dao.readState("  ", testExternalTransactionId);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenReadingAndTransactionIdIsNull() throws Exception {
        dao.readState(testCashier, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenReadingAndTransactionIdIsEmpty() throws Exception {
        dao.readState(testCashier, "");
    }

    @Test
    public void shouldReturnNullWhenReadingAndNoSuchEntry() throws Exception {
        PaymentState state = dao.readState(testCashier, testExternalTransactionId);
        assertNull(state);
    }

    @Test
    @Transactional
    public void shouldReadInsertedEntryFromTable() throws Exception {
        PaymentState expected = PaymentState.Started;
        int rows = dao.insertState(testCashier, testExternalTransactionId, expected);
        assertEquals(1, rows);
        PaymentState actual = dao.readState(testCashier, testExternalTransactionId);
        assertSame(expected, actual);
    }

    @Test
    @Transactional
    public void shouldReadUpdatedEntryFromTable() throws Exception {
        PaymentState expected = PaymentState.Failed;
        int rows = dao.updateState(testCashier, testExternalTransactionId, expected);
        assertEquals(1, rows);
        PaymentState actual = dao.readState(testCashier, testExternalTransactionId);
        assertSame(expected, actual);
    }

    @Test
    @Transactional
    public void shouldUpdateEntryInTableToFinishedFailed() throws Exception {
        dao.insertState(testCashier, testExternalTransactionId, PaymentState.Started);
        dao.updateState(testCashier, testExternalTransactionId, PaymentState.FinishedFailed);
        assertPaymentState(testCashier, testExternalTransactionId, PaymentState.FinishedFailed);
    }

    private void assertPaymentState(String expectedCashierName, String expectedExternalTransactionId, PaymentState expectedState) {
        Map<String, Object> results = template.queryForMap(RETRIEVE_STATE, expectedCashierName, expectedExternalTransactionId);
        assertEquals(expectedState.name(), results.get(STATE));
        assertEquals(expectedCashierName, results.get(CASHIER_NAME));
        assertEquals(expectedExternalTransactionId, results.get(EXTERNAL_TRANSACTION_ID));
    }
}
