package com.yazino.platform.persistence.account;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.payment.PendingSettlement;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
public class JDBCPaymentSettlementDAOIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-1000).setScale(2);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-100).setScale(2);
    private static final BigDecimal PRICE = BigDecimal.valueOf(200).setScale(4);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2100).setScale(4);
    private static final BigDecimal BASE_CURRENCY_AMOUNT = BigDecimal.valueOf(300).setScale(4);
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("1.6666666").setScale(7);
    private static final Long PROMOTION_ID = 3141592L;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JDBCPaymentSettlementDAO underTest;

    @BeforeClass
    public static void lockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(((System.currentTimeMillis() / 1000) * 1000) - 60000);//truncates the milliseconds
    }

    @AfterClass
    public static void unlockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aNonExistentPaymentSettlementReturnsAbsent() {
        final Optional<PaymentSettlement> paymentSettlement = underTest.findById(internalTxIdFor(2));

        assertThat(paymentSettlement.isPresent(), is(false));
    }

    @Test
    public void aPaymentSettlementCanBeFoundByTheInternalTransactionId() {
        underTest.save(aCompletePaymentSettlement(1));

        final Optional<PaymentSettlement> paymentSettlement = underTest.findById(internalTxIdFor(1));

        assertThat(paymentSettlement.isPresent(), is(true));
        assertThat(paymentSettlement.get(), is(equalTo(aCompletePaymentSettlement(1))));
    }

    @Test
    public void aNonExistentPaymentSettlementDoesNotCauseAnErrorOnDeletion() {
        underTest.deleteById(internalTxIdFor(1));
    }

    @Test
    public void aPaymentSettlementCanBeDeletedByTheInternalTransactionId() {
        underTest.save(aCompletePaymentSettlement(1));
        underTest.save(aCompletePaymentSettlement(2));

        underTest.deleteById(internalTxIdFor(1));

        final int recordCountForRecord1 = jdbcTemplate.queryForObject("SELECT count(1) FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?", Integer.class, internalTxIdFor(1));
        assertThat(recordCountForRecord1, is(equalTo(0)));
        final int recordCountForRecord2 = jdbcTemplate.queryForObject("SELECT count(1) FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?", Integer.class, internalTxIdFor(2));
        assertThat(recordCountForRecord2, is(equalTo(1)));
    }

    @Test
    public void findingSummarisedPendingSettlementsReturnsTheFirstPageOfSettlements() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(aCompletePaymentSettlement(i + 1));
        }

        final PagedData<PendingSettlement> pendingSettlements = underTest.findSummarisedPendingSettlements(0, 2);

        assertThat(pendingSettlements.getSize(), is(equalTo(2)));
        assertThat(pendingSettlements.getTotalSize(), is(equalTo(5)));
        assertThat(pendingSettlements, hasItems(summaryOf(aCompletePaymentSettlement(1)), summaryOf(aCompletePaymentSettlement(2))));
    }

    @Test
    public void findingSummarisedPendingSettlementsReturnsAnIntermediatePageOfSettlements() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(aCompletePaymentSettlement(i + 1));
        }

        final PagedData<PendingSettlement> pendingSettlements = underTest.findSummarisedPendingSettlements(1, 2);

        assertThat(pendingSettlements.getSize(), is(equalTo(2)));
        assertThat(pendingSettlements.getTotalSize(), is(equalTo(5)));
        assertThat(pendingSettlements, hasItems(summaryOf(aCompletePaymentSettlement(3)), summaryOf(aCompletePaymentSettlement(4))));
    }

    @Test
    public void findingSummarisedPendingSettlementsReturnsTheLastPageOfSettlements() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(aCompletePaymentSettlement(i + 1));
        }

        final PagedData<PendingSettlement> pendingSettlements = underTest.findSummarisedPendingSettlements(2, 2);

        assertThat(pendingSettlements.getSize(), is(equalTo(1)));
        assertThat(pendingSettlements.getTotalSize(), is(equalTo(5)));
        assertThat(pendingSettlements, hasItems(summaryOf(aCompletePaymentSettlement(5))));
    }

    @Test
    public void findingPendingSettlementsByPlayerIdReturnsAnEmptySetWhereNoneMatch() {
        final Set<PaymentSettlement> pendingSettlements = underTest.findByPlayerId(PLAYER_ID);

        assertThat(pendingSettlements.size(), is(equalTo(0)));
    }

    @Test
    public void findingPendingSettlementsByPlayerIdReturnsTheMatchingSettlements() {
        underTest.save(aCompletePaymentSettlementWithPlayer(1, PLAYER_ID));
        underTest.save(aCompletePaymentSettlementWithPlayer(2, BigDecimal.valueOf(-2000).setScale(2)));
        underTest.save(aCompletePaymentSettlementWithPlayer(3, PLAYER_ID));

        final Set<PaymentSettlement> pendingSettlements = underTest.findByPlayerId(PLAYER_ID);

        assertThat(pendingSettlements.size(), is(equalTo(2)));
        assertThat(pendingSettlements, hasItems(aCompletePaymentSettlement(1), aCompletePaymentSettlement(3)));
    }

    @Test
    public void findingSummarisedPendingSettlementsReturnsEmptyWhenNoResultsMatch() {
        final PagedData<PendingSettlement> pendingSettlements = underTest.findSummarisedPendingSettlements(2, 2);

        assertThat(pendingSettlements.getSize(), is(equalTo(0)));
        assertThat(pendingSettlements.getTotalSize(), is(equalTo(0)));
    }

    @Test
    public void findingPendingSettlementsReturnsOnlySettlementsRecordedBeforeTheSettlementDelay() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(aCompletePaymentSettlement(i + 1));
        }

        final Set<PaymentSettlement> pendingSettlements = underTest.findPendingSettlements(3);

        assertThat(pendingSettlements.size(), is(equalTo(3)));
        assertThat(pendingSettlements, hasItems(aCompletePaymentSettlement(3), aCompletePaymentSettlement(4), aCompletePaymentSettlement(5)));
    }

    @Test
    public void findingPendingSettlementsReturnsAnEmptySetWhenNoSettlementsArePending() {
        underTest.save(aCompletePaymentSettlement(1));
        underTest.save(aCompletePaymentSettlement(2));

        final Set<PaymentSettlement> pendingSettlements = underTest.findPendingSettlements(3);

        assertThat(pendingSettlements, is(not(nullValue())));
        assertThat(pendingSettlements.size(), is(equalTo(0)));
    }

    @Test
    public void aCompletePaymentSettlementCanBeInserted() {
        underTest.save(aCompletePaymentSettlement(1));

        final Map<String, Object> settlement = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?", internalTxIdFor(1));
        assertThat(settlement, is(not(nullValue())));
        assertThat(settlement.isEmpty(), is(not(true)));
        assertThat((String) settlement.get("INTERNAL_TRANSACTION_ID"), is(equalTo(internalTxIdFor(1))));
        assertThat((String) settlement.get("EXTERNAL_TRANSACTION_ID"), is(equalTo("anExternalTransaction")));
        assertThat((BigDecimal) settlement.get("ACCOUNT_ID"), is(equalTo(ACCOUNT_ID)));
        assertThat((String) settlement.get("CASHIER_NAME"), is(equalTo("testCashier")));
        assertThat((Timestamp) settlement.get("TRANSACTION_TS"), is(equalTo(new Timestamp(new DateTime().minusHours(1).getMillis()))));
        assertThat((String) settlement.get("ACCOUNT_NUMBER"), is(equalTo("anAccountNumber")));
        assertThat((BigDecimal) settlement.get("PRICE"), is(equalTo(PRICE)));
        assertThat((String) settlement.get("CURRENCY_CODE"), is(equalTo("GBP")));
        assertThat((BigDecimal) settlement.get("CHIPS"), is(equalTo(CHIPS)));
        assertThat((String) settlement.get("TRANSACTION_TYPE"), is(equalTo(ExternalTransactionType.DEPOSIT.name())));
        assertThat((String) settlement.get("GAME_TYPE"), is(equalTo("aGameType")));
        assertThat((String) settlement.get("PLATFORM"), is(equalTo("ANDROID")));
        assertThat((String) settlement.get("PAYMENT_OPTION_ID"), is(equalTo("aPaymentOptionId")));
        assertThat((BigDecimal) settlement.get("BASE_CURRENCY_AMOUNT"), is(equalTo(BASE_CURRENCY_AMOUNT)));
        assertThat((String) settlement.get("BASE_CURRENCY_CODE"), is(equalTo("USD")));
        assertThat((BigDecimal) settlement.get("EXCHANGE_RATE"), is(equalTo(EXCHANGE_RATE)));
    }

    @Test
    public void aMinimalPaymentSettlementCanBeInserted() {
        underTest.save(aMinimalPaymentSettlement(1));

        final Map<String, Object> settlement = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?", internalTxIdFor(1));
        assertThat(settlement, is(not(nullValue())));
        assertThat(settlement.isEmpty(), is(not(true)));
        assertThat((String) settlement.get("INTERNAL_TRANSACTION_ID"), is(equalTo(internalTxIdFor(1))));
        assertThat((String) settlement.get("EXTERNAL_TRANSACTION_ID"), is(equalTo("anExternalTransaction")));
        assertThat((BigDecimal) settlement.get("ACCOUNT_ID"), is(equalTo(ACCOUNT_ID)));
        assertThat((String) settlement.get("CASHIER_NAME"), is(equalTo("testCashier")));
        assertThat((Timestamp) settlement.get("TRANSACTION_TS"), is(equalTo(new Timestamp(new DateTime().minusHours(1).getMillis()))));
        assertThat((String) settlement.get("ACCOUNT_NUMBER"), is(equalTo("anAccountNumber")));
        assertThat((BigDecimal) settlement.get("PRICE"), is(equalTo(PRICE)));
        assertThat((String) settlement.get("CURRENCY_CODE"), is(equalTo("GBP")));
        assertThat((BigDecimal) settlement.get("CHIPS"), is(equalTo(CHIPS)));
        assertThat((String) settlement.get("TRANSACTION_TYPE"), is(equalTo(ExternalTransactionType.DEPOSIT.name())));
        assertThat(settlement.get("GAME_TYPE"), is(nullValue()));
        assertThat(settlement.get("PLATFORM"), is(nullValue()));
        assertThat(settlement.get("PAYMENT_OPTION_ID"), is(nullValue()));
        assertThat(settlement.get("BASE_CURRENCY_AMOUNT"), is(nullValue()));
        assertThat(settlement.get("BASE_CURRENCY_CODE"), is(nullValue()));
        assertThat(settlement.get("EXCHANGE_RATE"), is(nullValue()));
    }

    private PaymentSettlement aMinimalPaymentSettlement(final int id) {
        return PaymentSettlement.newSettlement(internalTxIdFor(id),
                "anExternalTransaction",
                PLAYER_ID,
                ACCOUNT_ID,
                "testCashier",
                new DateTime().minusHours(id),
                "anAccountNumber",
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT)
                .build();
    }

    private String internalTxIdFor(final int id) {
        return "jdbcPaymentSettlementDAO-it-" + id;
    }

    private PendingSettlement summaryOf(final PaymentSettlement settlement) {
        return new PendingSettlement(settlement.getTimestamp(),
                settlement.getInternalTransactionId(),
                settlement.getExternalTransactionId(),
                settlement.getPlayerId(),
                null,
                null,
                settlement.getCashierName(),
                settlement.getCurrency(),
                settlement.getPrice(),
                settlement.getChips(),
                settlement.getBaseCurrency(),
                settlement.getBaseCurrencyAmount());
    }

    private PaymentSettlement aCompletePaymentSettlement(final int id) {
        return aCompletePaymentSettlementWithPlayer(id, PLAYER_ID);
    }

    private PaymentSettlement aCompletePaymentSettlementWithPlayer(final int id, final BigDecimal playerId) {
        return PaymentSettlement.newSettlement(internalTxIdFor(id),
                "anExternalTransaction",
                playerId,
                ACCOUNT_ID,
                "testCashier",
                new DateTime().minusHours(id),
                "anAccountNumber",
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT)
                .withGameType("aGameType")
                .withPlatform(Platform.ANDROID)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .withBaseCurrencyAmount(BASE_CURRENCY_AMOUNT)
                .withBaseCurrency(Currency.getInstance("USD"))
                .withExchangeRate(EXCHANGE_RATE)
                .build();
    }

}
