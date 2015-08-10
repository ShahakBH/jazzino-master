package com.yazino.platform.payment;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.util.BigDecimals;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
public class JDBCPaymentDisputeDAOIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-1000).setScale(2);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-100).setScale(2);
    private static final BigDecimal PRICE = BigDecimal.valueOf(200).setScale(4);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2100).setScale(4);
    private static final Long PROMOTION_ID = 3141592L;
    private static final DateTime BASE_DATE = new DateTime(2013, 2, 5, 10, 15);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JDBCPaymentDisputeDAO underTest;

    @BeforeClass
    public static void lockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(((System.currentTimeMillis() / 1000) * 1000) - 60000); // truncates the milliseconds
    }

    @AfterClass
    public static void unlockTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aNonExistentPaymentDisputeReturnsAbsent() {
        final Optional<PaymentDispute> paymentDispute = underTest.findByInternalTransactionId(internalTxIdFor(2));

        assertThat(paymentDispute.isPresent(), is(false));
    }

    @Test
    public void aPaymentDisputeCanBeSaved() {
        underTest.save(anOpenDispute(1, PLAYER_ID));

        final Map<String, Object> settlement = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_DISPUTE WHERE INTERNAL_TRANSACTION_ID=?", internalTxIdFor(1));
        assertThat(settlement, is(not(nullValue())));
        assertThat(settlement.isEmpty(), is(not(true)));
        assertThat((String) settlement.get("INTERNAL_TRANSACTION_ID"), is(equalTo(internalTxIdFor(1))));
        assertThat((String) settlement.get("CASHIER_NAME"), is(equalTo("testCashier")));
        assertThat((String) settlement.get("EXTERNAL_TRANSACTION_ID"), is(equalTo("anExternalTransactionId")));
        assertThat((String) settlement.get("DISPUTE_STATUS"), is(equalTo(DisputeStatus.OPEN.name())));
        assertThat((Timestamp) settlement.get("DISPUTE_TS"), is(equalTo(new Timestamp(BASE_DATE.minusHours(1).getMillis()))));
        assertThat((BigDecimal) settlement.get("PRICE"), is(equalTo(PRICE)));
        assertThat((String) settlement.get("CURRENCY_CODE"), is(equalTo("GBP")));
        assertThat((BigDecimal) settlement.get("CHIPS"), is(equalTo(CHIPS)));
        assertThat((String) settlement.get("TRANSACTION_TYPE"), is(equalTo(ExternalTransactionType.DEPOSIT.name())));
        assertThat((String) settlement.get("GAME_TYPE"), is(equalTo("aGameType")));
        assertThat((String) settlement.get("PLATFORM"), is(equalTo(Platform.WEB.toString())));
        assertThat((String) settlement.get("PAYMENT_OPTION_ID"), is(equalTo("aPaymentOptionId")));
        assertThat((String) settlement.get("DESCRIPTION"), is(equalTo("aDisputeReason")));
        assertThat(settlement.get("RESOLUTION"), is(nullValue()));
        assertThat(settlement.get("RESOLUTION_TS"), is(nullValue()));
        assertThat(settlement.get("RESOLUTION_NOTE"), is(nullValue()));
        assertThat(settlement.get("RESOLVED_BY"), is(nullValue()));
    }

    @Test
    public void aPaymentDisputeCanBeUpdated() {
        underTest.save(anOpenDispute(1, PLAYER_ID));

        underTest.save(aResolvedDispute(1, PLAYER_ID));

        final Map<String, Object> settlement = jdbcTemplate.queryForMap("SELECT * FROM PAYMENT_DISPUTE WHERE INTERNAL_TRANSACTION_ID=?", internalTxIdFor(1));
        assertThat(settlement, is(not(nullValue())));
        assertThat(settlement.isEmpty(), is(not(true)));
        assertThat((String) settlement.get("INTERNAL_TRANSACTION_ID"), is(equalTo(internalTxIdFor(1))));
        assertThat((String) settlement.get("CASHIER_NAME"), is(equalTo("testCashier")));
        assertThat((String) settlement.get("EXTERNAL_TRANSACTION_ID"), is(equalTo("anExternalTransactionId")));
        assertThat((String) settlement.get("DISPUTE_STATUS"), is(equalTo(DisputeStatus.CLOSED.name())));
        assertThat((Timestamp) settlement.get("DISPUTE_TS"), is(equalTo(new Timestamp(BASE_DATE.minusHours(1).getMillis()))));
        assertThat((BigDecimal) settlement.get("PRICE"), is(equalTo(PRICE)));
        assertThat((String) settlement.get("CURRENCY_CODE"), is(equalTo("GBP")));
        assertThat((BigDecimal) settlement.get("CHIPS"), is(equalTo(CHIPS)));
        assertThat((String) settlement.get("TRANSACTION_TYPE"), is(equalTo(ExternalTransactionType.DEPOSIT.name())));
        assertThat((String) settlement.get("GAME_TYPE"), is(equalTo("aGameType")));
        assertThat((String) settlement.get("PLATFORM"), is(equalTo(Platform.WEB.toString())));
        assertThat((String) settlement.get("PAYMENT_OPTION_ID"), is(equalTo("aPaymentOptionId")));
        assertThat((String) settlement.get("DESCRIPTION"), is(equalTo("aDisputeReason")));
        assertThat((String) settlement.get("RESOLUTION"), is(equalTo(DisputeResolution.REFUNDED_FRAUD.name())));
        assertThat((Timestamp) settlement.get("RESOLUTION_TS"), is(equalTo(new Timestamp(BASE_DATE.getMillis()))));
        assertThat((String) settlement.get("RESOLUTION_NOTE"), is(equalTo("testResolution")));
        assertThat((String) settlement.get("RESOLVED_BY"), is(equalTo("aTester")));
    }

    @Test
    public void aPaymentDisputeCanBeFoundByTheInternalTransactionId() {
        underTest.save(anOpenDispute(1, PLAYER_ID));

        final Optional<PaymentDispute> paymentDispute = underTest.findByInternalTransactionId(internalTxIdFor(1));

        assertThat(paymentDispute.isPresent(), is(true));
        assertThat(paymentDispute.get(), is(equalTo(anOpenDispute(1, PLAYER_ID))));
        assertThat(paymentDispute.get().getStatus(), is(equalTo(DisputeStatus.OPEN)));
    }

    @Test
    public void anUpdatedPaymentDisputeCanBeFoundByTheInternalTransactionId() {
        underTest.save(anOpenDispute(1, PLAYER_ID));
        underTest.save(aResolvedDispute(1, PLAYER_ID));

        final Optional<PaymentDispute> paymentDispute = underTest.findByInternalTransactionId(internalTxIdFor(1));
        assertThat(paymentDispute.isPresent(), is(true));
        assertThat(paymentDispute.get(), is(equalTo(aResolvedDispute(1, PLAYER_ID))));
        assertThat(paymentDispute.get().getStatus(), is(equalTo(DisputeStatus.CLOSED)));
    }

    @Test
    public void findingOpenDisputesReturnsTheFirstPageOfDisputes() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(anOpenDispute(i + 1, PLAYER_ID));
        }

        final PagedData<DisputeSummary> openDisputes = underTest.findOpenDisputes(0, 2);

        assertThat(openDisputes.getSize(), is(equalTo(2)));
        assertThat(openDisputes.getTotalSize(), is(equalTo(5)));
        assertThat(openDisputes, hasItems(summaryOf(anOpenDispute(1, PLAYER_ID)), summaryOf(anOpenDispute(2, PLAYER_ID))));
    }

    @Test
    public void findingOpenDisputesReturnsAnIntermediatePageOfDisputes() {
        for (int i = 0; i < 5; ++i) {

            underTest.save(anOpenDispute(i + 1, PLAYER_ID));
        }

        final PagedData<DisputeSummary> openDisputes = underTest.findOpenDisputes(1, 2);

        assertThat(openDisputes.getSize(), is(equalTo(2)));
        assertThat(openDisputes.getTotalSize(), is(equalTo(5)));
        assertThat(openDisputes, hasItems(summaryOf(anOpenDispute(3, PLAYER_ID)), summaryOf(anOpenDispute(4, PLAYER_ID))));
    }

    @Test
    public void findingOpenDisputesReturnsTheLastPageOfDisputes() {
        for (int i = 0; i < 5; ++i) {
            underTest.save(anOpenDispute(i + 1, PLAYER_ID));
        }

        final PagedData<DisputeSummary> openDisputes = underTest.findOpenDisputes(2, 2);

        assertThat(openDisputes.getSize(), is(equalTo(1)));
        assertThat(openDisputes.getTotalSize(), is(equalTo(5)));
        assertThat(openDisputes, hasItems(summaryOf(anOpenDispute(5, PLAYER_ID))));
    }

    @Test
    public void findingOpenDisputesReturnsEmptyWhenNoResultsMatch() {
        final PagedData<DisputeSummary> openDisputes = underTest.findOpenDisputes(2, 2);

        assertThat(openDisputes.getSize(), is(equalTo(0)));
        assertThat(openDisputes.getTotalSize(), is(equalTo(0)));
    }

    private String internalTxIdFor(final int id) {
        return "jdbcPaymentDisputeDAO-it-" + id;
    }

    private DisputeSummary summaryOf(final PaymentDispute dispute) {
        return new DisputeSummary(dispute.getInternalTransactionId(),
                dispute.getCashierName(),
                dispute.getExternalTransactionId(),
                dispute.getStatus(),
                dispute.getDisputeTimestamp(),
                dispute.getPlayerId(),
                null,
                null,
                dispute.getCurrency(),
                dispute.getPrice(),
                dispute.getChips(),
                dispute.getDescription());
    }

    private PaymentDispute anOpenDispute(final int id, final BigDecimal playerId) {
        return PaymentDispute.newDispute(internalTxIdFor(id),
                "testCashier",
                "anExternalTransactionId",
                BigDecimals.strip(playerId),
                BigDecimals.strip(ACCOUNT_ID),
                BASE_DATE.minusHours(id),
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT,
                "aDisputeReason")
                .withGameType("aGameType")
                .withPlatform(Platform.WEB)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .build();
    }

    private PaymentDispute aResolvedDispute(final int id, final BigDecimal playerId) {
        return PaymentDispute.newDispute(internalTxIdFor(id),
                "testCashier",
                "anExternalTransactionId",
                BigDecimals.strip(playerId),
                BigDecimals.strip(ACCOUNT_ID),
                BASE_DATE.minusHours(id),
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT,
                "aDisputeReason")
                .withGameType("aGameType")
                .withPlatform(Platform.WEB)
                .withPaymentOptionId("aPaymentOptionId")
                .withPromotionId(PROMOTION_ID)
                .withResolution(DisputeResolution.REFUNDED_FRAUD,
                        BASE_DATE,
                        "testResolution",
                        "aTester")
                .build();
    }

}