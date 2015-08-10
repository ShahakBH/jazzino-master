package com.yazino.bi.payment.persistence;

import com.yazino.bi.payment.Chargeback;
import com.yazino.platform.model.PagedData;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Currency;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class JDBCPaymentChargebackDAOIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal AMOUNT = new BigDecimal("123.45");
    private static final BigDecimal MODIFIED_AMOUNT = new BigDecimal("123.40");
    private static final DateTime PROCESSING_DATE = new DateTime(2013, 1, 2, 0, 0, 0, 0);
    private static final DateTime TRANSACTION_DATE = new DateTime(2012, 12, 10, 0, 0, 0, 0);
    private static final String REASON_CODE = "zzzz";
    private static final String MODIFIED_REASON_CODE = "xxxx";
    private static final Currency CURRENCY = Currency.getInstance("GBP");

    @Autowired
    @Qualifier("marketingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JDBCPaymentChargebackDAO underTest;

    @Before
    public void cleanUpTable() {
        jdbcTemplate.update("DELETE FROM PAYMENT_CHARGEBACK");
    }

    @Test(expected = NullPointerException.class)
    public void savingANullChargebackThrowsANullPointerException() {
        underTest.save(null);
    }

    @Test
    public void savingAChargebackPersistsItToTheDatabase() {
        underTest.save(aChargeback());

        final Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_CHARGEBACK WHERE CHARGEBACK_REFERENCE=?", "aReference");
        assertThat((String) result.get("CHARGEBACK_REFERENCE"), is(equalTo("aReference")));
        assertThat((Date) result.get("PROCESSING_DATE"), is(equalTo(new Date(PROCESSING_DATE.getMillis()))));
        assertThat((String) result.get("INTERNAL_TRANSACTION_ID"), is(equalTo("anInternalTransactionId")));
        assertThat((Date) result.get("TRANSACTION_DATE"), is(equalTo(new Date(TRANSACTION_DATE.getMillis()))));
        assertThat((BigDecimal) result.get("PLAYER_ID"), is(equalTo(PLAYER_ID.setScale(2))));
        assertThat((String) result.get("CHARGEBACK_REASON_CODE"), is(equalTo(REASON_CODE)));
        assertThat((String) result.get("CHARGEBACK_REASON"), is(equalTo("aReason")));
        assertThat((String) result.get("ACCOUNT_NUMBER"), is(equalTo("anAccountNumber")));
        assertThat((BigDecimal) result.get("AMOUNT"), is(equalTo(AMOUNT.setScale(4))));
        assertThat((String) result.get("CURRENCY_CODE"), is(equalTo(CURRENCY.getCurrencyCode())));
    }

    @Test
    public void updatingAChargebackPersistsReasonAndAmountToTheDatabase() {
        underTest.save(aChargeback());

        underTest.save(aModifiedChargeback());

        final Map<String, Object> result = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_CHARGEBACK WHERE CHARGEBACK_REFERENCE=?", "aReference");
        assertThat((String) result.get("CHARGEBACK_REFERENCE"), is(equalTo("aReference")));
        assertThat((Date) result.get("PROCESSING_DATE"), is(equalTo(new Date(PROCESSING_DATE.getMillis()))));
        assertThat((String) result.get("INTERNAL_TRANSACTION_ID"), is(equalTo("anInternalTransactionId")));
        assertThat((Date) result.get("TRANSACTION_DATE"), is(equalTo(new Date(TRANSACTION_DATE.getMillis()))));
        assertThat((BigDecimal) result.get("PLAYER_ID"), is(equalTo(PLAYER_ID.setScale(2))));
        assertThat((String) result.get("CHARGEBACK_REASON_CODE"), is(equalTo(MODIFIED_REASON_CODE)));
        assertThat((String) result.get("CHARGEBACK_REASON"), is(equalTo("aModifiedReason")));
        assertThat((String) result.get("ACCOUNT_NUMBER"), is(equalTo("anAccountNumber")));
        assertThat((BigDecimal) result.get("AMOUNT"), is(equalTo(MODIFIED_AMOUNT.setScale(4))));
        assertThat((String) result.get("CURRENCY_CODE"), is(equalTo(CURRENCY.getCurrencyCode())));
    }

    @Test
    public void searchingForChargebacksByDateWhereNoRecordsMatchReturnsAnEmptyList() {
        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 4), dateAt(2013, 9, 5), null, 0, 20);

        assertThat(results.getData(), is(empty()));
    }

    @Test
    public void searchingForChargebacksByDateAndReasonWhereNoRecordsMatchReturnsAnEmptyList() {
        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 4), dateAt(2013, 9, 5), asList("aReason"), 0, 20);

        assertThat(results.getData(), is(empty()));
    }

    @Test
    public void searchingForChargebacksByProcessingDateReturnsAllNewerOrEqualToTheDate() {
        for (int i = 1; i <= 7; ++i) {
            underTest.save(aChargeback("reference" + i, dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 4), dateAt(2013, 9, 7), null, 0, 20);

        assertThat(results.getData(), contains(aChargeback("reference4", dateAt(2013, 9, 4)),
                aChargeback("reference5", dateAt(2013, 9, 5)),
                aChargeback("reference6", dateAt(2013, 9, 6)),
                aChargeback("reference7", dateAt(2013, 9, 7))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateReturnsAllNewerOrEqualToTheDateForTheFirstPage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("ref%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), null, 0, 5);

        assertThat(results.getData(), contains(aChargeback("ref01", dateAt(2013, 9, 1)),
                aChargeback("ref02", dateAt(2013, 9, 2)),
                aChargeback("ref03", dateAt(2013, 9, 3)),
                aChargeback("ref04", dateAt(2013, 9, 4)),
                aChargeback("ref05", dateAt(2013, 9, 5))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateReturnsAllNewerOrEqualToTheDateForAMiddlePage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("ref%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), null, 1, 5);

        assertThat(results.getData(), contains(aChargeback("ref06", dateAt(2013, 9, 6)),
                aChargeback("ref07", dateAt(2013, 9, 7)),
                aChargeback("ref08", dateAt(2013, 9, 8)),
                aChargeback("ref09", dateAt(2013, 9, 9)),
                aChargeback("ref10", dateAt(2013, 9, 10))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateReturnsAllNewerOrEqualToTheDateForTheFinalPage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("ref%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), null, 2, 5);

        assertThat(results.getData(), contains(aChargeback("ref11", dateAt(2013, 9, 11)),
                aChargeback("ref12", dateAt(2013, 9, 12)),
                aChargeback("ref13", dateAt(2013, 9, 13))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateAndReasonReturnsAllNewerOrEqualToTheDateWithAMatchingReason() {
        for (int i = 1; i <= 7; ++i) {
            underTest.save(aChargeback("refX" + i, dateAt(2013, 9, i)));
            underTest.save(aModifiedChargeback("refY" + i, dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 4), dateAt(2013, 9, 7), asList(MODIFIED_REASON_CODE), 0, 20);

        assertThat(results.getData(), contains(aModifiedChargeback("refY4", dateAt(2013, 9, 4)),
                aModifiedChargeback("refY5", dateAt(2013, 9, 5)),
                aModifiedChargeback("refY6", dateAt(2013, 9, 6)),
                aModifiedChargeback("refY7", dateAt(2013, 9, 7))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateAndReasonReturnsAllNewerOrEqualToTheDateWithAMatchingReasonWithinTheFinalPage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("refX%1$02d", i), dateAt(2013, 9, i)));
            underTest.save(aModifiedChargeback(String.format("refY%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), asList(MODIFIED_REASON_CODE), 2, 5);

        assertThat(results.getData(), contains(aModifiedChargeback("refY11", dateAt(2013, 9, 11)),
                aModifiedChargeback("refY12", dateAt(2013, 9, 12)),
                aModifiedChargeback("refY13", dateAt(2013, 9, 13))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateAndReasonReturnsAllNewerOrEqualToTheDateWithAMatchingReasonWithinAMiddlePage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("refX%1$02d", i), dateAt(2013, 9, i)));
            underTest.save(aModifiedChargeback(String.format("refY%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), asList(MODIFIED_REASON_CODE), 1, 5);

        assertThat(results.getData(), contains(aModifiedChargeback("refY06", dateAt(2013, 9, 6)),
                aModifiedChargeback("refY07", dateAt(2013, 9, 7)),
                aModifiedChargeback("refY08", dateAt(2013, 9, 8)),
                aModifiedChargeback("refY09", dateAt(2013, 9, 9)),
                aModifiedChargeback("refY10", dateAt(2013, 9, 10))));
    }

    @Test
    public void searchingForChargebacksByProcessingDateAndReasonReturnsAllNewerOrEqualToTheDateWithAMatchingReasonWithinTheFirstPage() {
        for (int i = 1; i <= 13; ++i) {
            underTest.save(aChargeback(String.format("refX%1$02d", i), dateAt(2013, 9, i)));
            underTest.save(aModifiedChargeback(String.format("refY%1$02d", i), dateAt(2013, 9, i)));
        }

        final PagedData<Chargeback> results = underTest.search(dateAt(2013, 9, 1), dateAt(2013, 9, 13), asList(MODIFIED_REASON_CODE), 0, 5);

        assertThat(results.getData(), contains(aModifiedChargeback("refY01", dateAt(2013, 9, 1)),
                aModifiedChargeback("refY02", dateAt(2013, 9, 2)),
                aModifiedChargeback("refY03", dateAt(2013, 9, 3)),
                aModifiedChargeback("refY04", dateAt(2013, 9, 4)),
                aModifiedChargeback("refY05", dateAt(2013, 9, 5))));
    }

    private DateTime dateAt(final int year, final int monthOfYear, final int dayOfMonth) {
        return new DateTime(year, monthOfYear, dayOfMonth, 0, 0, 0);
    }

    private Chargeback aChargeback() {
        return aChargeback("aReference", PROCESSING_DATE);
    }

    private Chargeback aChargeback(final String reference,
                                   final DateTime processingDate) {
        return new Chargeback(reference,
                processingDate,
                "anInternalTransactionId",
                TRANSACTION_DATE,
                PLAYER_ID,
                null, REASON_CODE,
                "aReason",
                "anAccountNumber",
                AMOUNT,
                CURRENCY);
    }

    private Chargeback aModifiedChargeback() {
        return aModifiedChargeback("aReference", PROCESSING_DATE);
    }

    private Chargeback aModifiedChargeback(final String reference,
                                           final DateTime processingDate) {
        return new Chargeback(reference,
                processingDate,
                "anInternalTransactionId",
                TRANSACTION_DATE,
                PLAYER_ID,
                null,
                MODIFIED_REASON_CODE,
                "aModifiedReason",
                "anAccountNumber",
                MODIFIED_AMOUNT,
                CURRENCY);
    }
}
