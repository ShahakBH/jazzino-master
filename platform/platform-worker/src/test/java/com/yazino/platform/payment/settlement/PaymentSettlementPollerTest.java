package com.yazino.platform.payment.settlement;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Currency;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PaymentSettlementPollerTest {
    private static final int SETTLEMENT_DELAY_HOURS = 31;
    private static final int DEFAULT_SETTLEMENT_DELAY_IN_HOURS = 36;

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private JDBCPaymentSettlementDAO paymentSettlementDao;
    @Mock
    private QueuePublishingService<PaymentSettlementRequest> publisher;

    private PaymentSettlementPoller underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(200000000L);

        when(yazinoConfiguration.getInt("payment.worldpay.stlink.settlement-delay-hours", DEFAULT_SETTLEMENT_DELAY_IN_HOURS))
                .thenReturn(SETTLEMENT_DELAY_HOURS);

        underTest = new PaymentSettlementPoller(yazinoConfiguration, paymentSettlementDao, publisher);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void thePollShouldRunEveryTenMinutes() throws NoSuchMethodException {
        final Method pollMethod = PaymentSettlementPoller.class.getMethod("enqueuePaymentsDueForSettlement");
        final Scheduled scheduledAnnotation = pollMethod.getAnnotation(Scheduled.class);

        assertThat(scheduledAnnotation, is(not(nullValue())));
        assertThat(scheduledAnnotation.fixedDelay(), is(equalTo(10L * 60 * 1000)));
    }

    @Test
    public void thePollQueriesForPendingSettlementsWithTheConfiguredDelay() {
        when(paymentSettlementDao.findPendingSettlements(anyInt())).thenReturn(Collections.<PaymentSettlement>emptySet());

        underTest.enqueuePaymentsDueForSettlement();

        verify(paymentSettlementDao).findPendingSettlements(SETTLEMENT_DELAY_HOURS);
    }

    @Test
    public void thePollQueriesForPendingSettlementsWithTheADelayOf36HoursIfTheDelayPropertyIsMissing() {
        reset(yazinoConfiguration);
        when(paymentSettlementDao.findPendingSettlements(anyInt())).thenReturn(Collections.<PaymentSettlement>emptySet());
        when(yazinoConfiguration.getInt("payment.worldpay.stlink.settlement-delay-hours", DEFAULT_SETTLEMENT_DELAY_IN_HOURS))
                .thenReturn(DEFAULT_SETTLEMENT_DELAY_IN_HOURS);

        underTest.enqueuePaymentsDueForSettlement();

        verify(yazinoConfiguration).getInt("payment.worldpay.stlink.settlement-delay-hours", DEFAULT_SETTLEMENT_DELAY_IN_HOURS);
        verify(paymentSettlementDao).findPendingSettlements(DEFAULT_SETTLEMENT_DELAY_IN_HOURS);
    }

    @Test
    public void exceptionsFromTheDAOAreNotPropagated() {
        when(paymentSettlementDao.findPendingSettlements(anyInt())).thenThrow(new RuntimeException("aTestException"));

        underTest.enqueuePaymentsDueForSettlement();

        verifyZeroInteractions(publisher);
    }

    @Test
    public void exceptionsFromThePublisherAreNotPropagated() {
        when(paymentSettlementDao.findPendingSettlements(anyInt())).thenReturn(newHashSet(aPaymentSettlement(1)));
        doThrow(new RuntimeException("aTestException")).when(publisher).send(aPaymentSettlementRequest(1));

        underTest.enqueuePaymentsDueForSettlement();
    }

    @Test
    public void noSettlementsAreEnqueuedWhenThereAreNoPendingSettlements() {
        when(paymentSettlementDao.findPendingSettlements(anyInt())).thenReturn(Collections.<PaymentSettlement>emptySet());

        underTest.enqueuePaymentsDueForSettlement();

        verifyZeroInteractions(publisher);
    }

    @Test
    public void pendingSettlementsAreEnqueued() {
        when(paymentSettlementDao.findPendingSettlements(anyInt()))
                .thenReturn(newHashSet(aPaymentSettlement(1), aPaymentSettlement(2), aPaymentSettlement(3)));

        underTest.enqueuePaymentsDueForSettlement();

        verify(publisher).send(aPaymentSettlementRequest(1));
        verify(publisher).send(aPaymentSettlementRequest(2));
        verify(publisher).send(aPaymentSettlementRequest(3));
    }

    private PaymentSettlementRequest aPaymentSettlementRequest(final int id) {
        return new PaymentSettlementRequest("internalTx" + id);
    }

    private PaymentSettlement aPaymentSettlement(final int id) {
        return PaymentSettlement.newSettlement("internalTx" + id,
                "externalTx" + id,
                BigDecimal.valueOf(100 - id),
                BigDecimal.valueOf(0 - id),
                "aCashier",
                new DateTime(),
                "anAccountNumber",
                BigDecimal.valueOf(10),
                Currency.getInstance("GBP"),
                BigDecimal.valueOf(200),
                ExternalTransactionType.DEPOSIT)
                .withGameType("aGameType")
                .withPlatform(Platform.IOS)
                .withPaymentOptionId("aPaymentOptionId")
                .withBaseCurrencyAmount(BigDecimal.valueOf(6))
                .withBaseCurrency(Currency.getInstance("NOK"))
                .withExchangeRate(new BigDecimal("0.6"))
                .build();
    }

}
