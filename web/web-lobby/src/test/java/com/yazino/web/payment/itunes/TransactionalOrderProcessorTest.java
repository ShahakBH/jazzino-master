package com.yazino.web.payment.itunes;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TransactionalOrderProcessorTest {

    private final String mCashierName = "TestCashier";
    private final String mExternalTransactionId = "TestExternalTransactionId";
    private final PaymentStateService mPaymentStateService = mock(PaymentStateService.class);
    private final OrderProcessor<Order> mOrderProcessor = mock(OrderProcessor.class);
    private final TransactionalOrderProcessor<Order> mProcessor = new TransactionalOrderProcessor<Order>(mPaymentStateService, mOrderProcessor);
    private final Order mOrder = mock(Order.class);

    @Before
    public void setup() {
        when(mOrder.getCashier()).thenReturn(mCashierName);
        when(mOrder.getOrderId()).thenReturn(mExternalTransactionId);
    }

    @Test
    public void shouldNotRecordOrderIfPaymentStateIsUnknown() throws Exception {
        doThrow(new PaymentStateException(PaymentState.Unknown)).when(mPaymentStateService).startPayment(mCashierName, mExternalTransactionId);
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Unknown);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verifyZeroInteractions(mOrderProcessor);
    }
        
    @Test
    public void shouldNotRecordOrderIfPaymentStateIsFinished() throws Exception {
        doThrow(new PaymentStateException(PaymentState.Finished)).when(mPaymentStateService).startPayment(mCashierName, mExternalTransactionId);
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Finished);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldNotRecordOrderIfPaymentStateIsFailed() throws Exception {
        doThrow(new PaymentStateException(PaymentState.Failed)).when(mPaymentStateService).startPayment(mCashierName, mExternalTransactionId);
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Failed);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldNotRecordOrderIfPaymentStateIsStarted() throws Exception {
        doThrow(new PaymentStateException(PaymentState.Started)).when(mPaymentStateService).startPayment(mCashierName, mExternalTransactionId);
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Failed);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldFailPaymentIfRecorderThrowsException() throws Exception {
        doThrow(new WalletServiceException("TestException")).when(mOrderProcessor).processOrder(mOrder);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verify(mPaymentStateService).failPayment(mCashierName, mExternalTransactionId);
    }

    @Test
    public void shouldFinishPaymentIfRecorderSucceeds() throws Exception {
        when(mOrderProcessor.processOrder(mOrder)).thenReturn(true);
        boolean processed = mProcessor.processOrder(mOrder);
        assertTrue(processed);
        verify(mPaymentStateService).finishPayment(mCashierName, mExternalTransactionId);
    }

    @Test
    public void shouldNotThrowExceptionIfFinishingPaymentFails() throws Exception {
        when(mOrderProcessor.processOrder(mOrder)).thenReturn(true);
        doThrow(new PaymentStateException(PaymentState.Unknown)).when(mPaymentStateService).finishPayment(mCashierName, mExternalTransactionId);
        boolean processed = mProcessor.processOrder(mOrder);
        assertTrue(processed);
        verify(mPaymentStateService).finishPayment(mCashierName, mExternalTransactionId);
        verify(mOrder).setPaymentState(PaymentState.Unknown);
    }

    @Test
    public void shouldNotThrowExceptionIfFailingPaymentFails() throws Exception {
        doThrow(new PaymentStateException(PaymentState.Unknown)).when(mPaymentStateService).failPayment(mCashierName, mExternalTransactionId);
        doThrow(new WalletServiceException("TestException")).when(mOrderProcessor).processOrder(mOrder);
        boolean processed = mProcessor.processOrder(mOrder);
        assertFalse(processed);
        verify(mPaymentStateService).failPayment(mCashierName, mExternalTransactionId);
        verify(mOrder).setPaymentState(PaymentState.Unknown);
    }

    @Test
    public void shouldSetPaymentStateToFinishedIfPaymentFinishedOk() throws Exception {
        when(mOrderProcessor.processOrder(mOrder)).thenReturn(true);
        boolean processed = mProcessor.processOrder(mOrder);
        assertTrue(processed);
        verify(mOrder).setPaymentState(PaymentState.Finished);
    }

}
