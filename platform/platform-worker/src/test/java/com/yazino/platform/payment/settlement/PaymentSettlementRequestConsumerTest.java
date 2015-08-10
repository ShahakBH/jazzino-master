package com.yazino.platform.payment.settlement;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
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

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PaymentSettlementRequestConsumerTest {
    @Mock
    private WalletService walletService;
    @Mock
    private JDBCPaymentSettlementDAO paymentSettlementDao;
    @Mock
    private PaymentSettlementProcessor processor1;
    @Mock
    private ExternalTransaction externalTransaction1;
    @Mock
    private PaymentSettlementProcessor processor2;
    @Mock
    private ExternalTransaction externalTransaction2;

    private final Map<String, PaymentSettlementProcessor> processors = new HashMap<>();

    private PaymentSettlementRequestConsumer underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(200000000L);

        processors.put("cashier1", processor1);
        processors.put("cashier2", processor2);

        when(paymentSettlementDao.findById(anyString())).thenReturn(Optional.<PaymentSettlement>absent());
        when(paymentSettlementDao.findById(internalTransactionIdFor(1))).thenReturn(Optional.fromNullable(aPaymentSettlementWithCashier(1, "cashier1")));
        when(paymentSettlementDao.findById(internalTransactionIdFor(2))).thenReturn(Optional.fromNullable(aPaymentSettlementWithCashier(2, "cashier2")));
        when(paymentSettlementDao.findById(internalTransactionIdFor(3))).thenReturn(Optional.fromNullable(aPaymentSettlementWithCashier(3, "cashier3")));

        when(processor1.settle(aPaymentSettlementWithCashier(1, "cashier1"))).thenReturn(externalTransaction1);
        when(processor1.settle(aPaymentSettlementWithCashier(2, "cashier2"))).thenReturn(externalTransaction2);

        when(externalTransaction1.getStatus()).thenReturn(ExternalTransactionStatus.SETTLED);
        when(externalTransaction2.getStatus()).thenReturn(ExternalTransactionStatus.SETTLED);

        underTest = new PaymentSettlementRequestConsumer(paymentSettlementDao, walletService);
        underTest.setPaymentSettlementProcessors(processors);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aNullMessageIsIgnored() {
        underTest.handle(null);

        verifyZeroInteractions(walletService, paymentSettlementDao, processor1, processor2);
    }

    @Test(expected = RuntimeException.class)
    public void anExceptionWhenLookingUpASettlementIsPropagated() {
        when(paymentSettlementDao.findById(internalTransactionIdFor(20))).thenThrow(new RuntimeException("aTestException"));

        underTest.handle(aPaymentSettlementRequest(20));
    }

    @Test
    public void anExceptionWhenProcessingASettlementIsNotPropagated() throws WalletServiceException {
        when(processor1.settle(any(PaymentSettlement.class))).thenThrow(new RuntimeException("aTestException"));

        underTest.handle(aPaymentSettlementRequest(1));
    }

    @Test
    public void anExceptionWhenRecordingAnExternalTransactionIsNotPropagated() throws WalletServiceException {
        doThrow(new RuntimeException("aTestException")).when(walletService).record(any(ExternalTransaction.class));

        underTest.handle(aPaymentSettlementRequest(1));
    }

    @Test
    public void anExceptionWhenDeletingASettlementIsNotPropagated() throws WalletServiceException {
        doThrow(new RuntimeException("aTestException")).when(paymentSettlementDao).deleteById(anyString());

        underTest.handle(aPaymentSettlementRequest(1));
    }

    @Test
    public void aMessageForANonExistentTransactionIsIgnored() {
        underTest.handle(aPaymentSettlementRequest(10));

        verify(paymentSettlementDao).findById(internalTransactionIdFor(10));
        verifyNoMoreInteractions(paymentSettlementDao);
        verifyZeroInteractions(walletService, processor1, processor2);
    }

    @Test
    public void aMessageForACashierWithNoProcessorIsIgnored() {
        underTest.handle(aPaymentSettlementRequest(3));

        verify(paymentSettlementDao).findById(internalTransactionIdFor(3));
        verifyZeroInteractions(walletService, processor1, processor2);
    }

    @Test
    public void aMessageForACashierIsPassedToTheCorrectProcessor() {
        underTest.handle(aPaymentSettlementRequest(2));

        verify(processor2).settle(aPaymentSettlementWithCashier(2, "cashier2"));
        verifyZeroInteractions(processor1);
    }

    @Test
    public void anExternalTransactionIsRecordedAfterProcessing() throws WalletServiceException {
        underTest.handle(aPaymentSettlementRequest(1));

        verify(walletService).record(externalTransaction1);
    }

    @Test
    public void theSettlementIsDeletedFromThePendingRecordsAfterProcessing() throws WalletServiceException {
        underTest.handle(aPaymentSettlementRequest(1));

        verify(paymentSettlementDao).deleteById(internalTransactionIdFor(1));
    }

    @Test
    public void aSettlementInErrorIsNotDeletedFromThePendingRecordsAfterProcessing() throws WalletServiceException {
        reset(externalTransaction1);
        when(externalTransaction1.getStatus()).thenReturn(ExternalTransactionStatus.ERROR);

        underTest.handle(aPaymentSettlementRequest(1));

        verify(paymentSettlementDao, times(0)).deleteById(anyString());
    }

    private PaymentSettlementRequest aPaymentSettlementRequest(final int id) {
        return new PaymentSettlementRequest(internalTransactionIdFor(id));
    }

    private PaymentSettlement aPaymentSettlementWithCashier(final int id, final String cashier) {
        return PaymentSettlement.newSettlement(internalTransactionIdFor(id),
                "externalTx" + id,
                BigDecimal.valueOf(100 - id),
                BigDecimal.valueOf(0 - id),
                cashier,
                new DateTime(),
                "anAccountNumber",
                BigDecimal.valueOf(10),
                Currency.getInstance("GBP"),
                BigDecimal.valueOf(200),
                ExternalTransactionType.DEPOSIT)
                .withGameType("aGameType")
                .withPlatform(Platform.FACEBOOK_CANVAS)
                .withPaymentOptionId("aPaymentOptionId")
                .withBaseCurrencyAmount(BigDecimal.valueOf(6))
                .withBaseCurrency(Currency.getInstance("NOK"))
                .withExchangeRate(new BigDecimal("0.6"))
                .build();
    }

    private String internalTransactionIdFor(final int id) {
        return "internalTx" + id;
    }

}
