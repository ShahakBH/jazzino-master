package com.yazino.bi.operations.persistence;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JdbcPaymentTransactionReportDaoTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private PaymentTransactionReportDao underTest;

    @Before
    public void init() {
        underTest = new JdbcPaymentTransactionReportDao(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildDateRangeQuery() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", "", "");

        // THEN the correct queries are ran by JDBC
        final String query = JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query),
                eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate()}), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildDateRangeAndStatusQuery() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String status = "SUCCESS";

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", "", status);

        // THEN the correct queries are ran by JDBC
        final String query =
                JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_STATUS_CLAUSE
                        + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query),
                eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), status}),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildStatusQueryForSuccessfulTransactions() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String status = PaymentTransactionReportDao.SUCCESSFUL_STATUS;

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", "", status);

        // THEN the correct queries are ran by JDBC
        final String query = JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_SUCCESSFUL_STATUS_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;

        verify(jdbcTemplate).query(eq(query), eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate()}), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildCashierClauseForPurchases() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String status = PaymentTransactionReportDao.SUCCESSFUL_STATUS;

        final String cashier = PaymentTransactionReportDao.PURCHASE_TRANSACTIONS;

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", cashier, "");

        // THEN the correct queries are ran by JDBC
        final String query = JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CASHIER_PAYMENTS_CLAUSE+ JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query), eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), true}), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildCashierClauseForOffers() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String status = PaymentTransactionReportDao.SUCCESSFUL_STATUS;

        final String cashier = PaymentTransactionReportDao.OFFER_TRANSACTIONS;

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", cashier, "");

        // THEN the correct queries are ran by JDBC
        final String query = JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CASHIER_PAYMENTS_CLAUSE+ JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query), eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), false}), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildDateRangeAndCurrencyQuery() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String currency = "USD";

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, currency, "", "");

        // THEN the correct queries are ran by JDBC
        final String query =
                JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CURRENCY_CLAUSE
                        + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query),
                eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), currency}),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildDateRangeAndCashierQuery() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String cashier = "Wirecard";

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, "", cashier, "");

        // THEN the correct queries are ran by JDBC
        final String query =
                JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CASHIER_CLAUSE
                        + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate).query(eq(query),
                eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), cashier}),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildDateRangeStatusCurrencyCashierQuery() {
        // GIVEN the request data
        final DateTime fromDate = new DateTime(2011, 3, 4, 0, 0, 0, 0);
        final DateTime toDate = new DateTime(2011, 3, 5, 0, 0, 0, 0);
        final String cashier = "Wirecard";
        final String currency = "USD";
        final String status = "SUCCESS";

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(fromDate, toDate, currency, cashier, status);

        // THEN the correct queries are ran by JDBC
        final String query =
                JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_DATE_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_STATUS_CLAUSE
                        + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CURRENCY_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_CASHIER_CLAUSE + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ORDER_BY;
        verify(jdbcTemplate)
                .query(eq(query),
                        eq(new Object[]{fromDate.toDate(), toDate.plusDays(1).toDate(), status, currency,
                                cashier}), any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getPaymentTransactionDataShouldBuildIdQuery() {
        // GIVEN the request data
        final String transactionId = "13423243245abc";

        // WHEN querying the DAO
        underTest.getPaymentTransactionData(transactionId);

        // THEN the correct queries are ran by JDBC
        final String query = JdbcPaymentTransactionReportDao.PAYMENT_TXN_SELECT + JdbcPaymentTransactionReportDao.PAYMENT_TXN_ID_CLAUSE;
        verify(jdbcTemplate).query(eq(query), eq(new Object[]{transactionId, transactionId}),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCorrectlyMapPaymentTransactionData() throws SQLException {
        // GIVEN the query using mocked result set
        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<RowMapper> rowMapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        given(jdbcTemplate.query(anyString(), any(Object[].class), rowMapperCaptor.capture())).willReturn(
                null);

        // AND the result sets returning correct data
        given(resultSet.getDouble("amount")).willReturn(300d);
        given(resultSet.getString("currency_code")).willReturn("EUR");
        given(resultSet.getString("date")).willReturn("06/06/2011");
        given(resultSet.getString("failure_reason")).willReturn("a reason");
        given(resultSet.getString("internal_transaction_id")).willReturn("internal id");
        given(resultSet.getString("external_transaction_id")).willReturn("external id");
        given(resultSet.getLong("player_id")).willReturn(134222l);
        given(resultSet.getString("playerName")).willReturn("alan sugar");
        given(resultSet.getString("external_transaction_status")).willReturn("FAILURE");
        given(resultSet.getString("cashier_name")).willReturn("Wirecard");
        given(resultSet.getString("country")).willReturn("UK");
        given(resultSet.getString("game_type")).willReturn("SpinDaBottle");

        // WHEN requesting the payment transaction data
        underTest.getPaymentTransactionData(new DateTime(), new DateTime(), "", "", "");

        // AND invoking the mapper
        final PaymentTransactionData data =
                (PaymentTransactionData) rowMapperCaptor.getValue().mapRow(resultSet, 0);

        // THEN the resulting data set is filled correctly
        assertThat(data.getAmount(), is(300d));
        assertThat(data.getCurrencyCode(), is("EUR"));
        assertThat(data.getDate(), is("06/06/2011"));
        assertThat(data.getDetails(), is("a reason"));
        assertThat(data.getExternalId(), is("external id"));
        assertThat(data.getInternalId(), is("internal id"));
        assertThat(data.getPlayerId(), is(134222l));
        assertThat(data.getPlayerName(), is("alan sugar"));
        assertThat(data.getTxnStatus(), is("FAILURE"));
        assertThat(data.getCashier(), is("Wirecard"));
        assertThat(data.getRegCountry(), is("UK"));
        assertThat(data.getGameType(), is("SpinDaBottle"));
    }
}
