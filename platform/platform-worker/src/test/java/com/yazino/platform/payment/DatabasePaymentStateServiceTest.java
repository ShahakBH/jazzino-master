package com.yazino.platform.payment;

import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import java.sql.SQLException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DatabasePaymentStateServiceTest {

    private final JDBCPaymentStateDao dao = mock(JDBCPaymentStateDao .class);
    private final PaymentStateService service = new DatabasePaymentStateService(dao);

    private final String cashierName = "Foo";
    private final String externalTransactionId = "Bar";

    @Test
    public void shouldUseDaoToStartPayment() throws Exception {
        service.startPayment(cashierName, externalTransactionId);
        verify(dao).insertState(cashierName, externalTransactionId, PaymentState.Started);
    }

    @Test
    public void shouldThrowExceptionWithExistingStateWhenStartingAndPaymentExists() throws Exception {
        doThrow(new DataIntegrityViolationException("Not Connected")).when(dao).insertState(cashierName, externalTransactionId, PaymentState.Started);
        when(dao.readState(cashierName, externalTransactionId)).thenReturn(PaymentState.Failed);
        try {
            service.startPayment(cashierName, externalTransactionId);
            fail("expected PaymentStateException");
        } catch (PaymentStateException e) {
            assertSame(PaymentState.Failed, e.getState());
        }
    }

    @Test
    public void shouldThrowExceptionWithUnknownStateWhenStartingAndDataSourceNotConnected() throws Exception {
        CannotGetJdbcConnectionException notConnectedException = new CannotGetJdbcConnectionException("Not Connected", new SQLException());
        doThrow(notConnectedException).when(dao).insertState(cashierName, externalTransactionId, PaymentState.Started);
        doThrow(notConnectedException).when(dao).readState(cashierName, externalTransactionId);
        try {
            service.startPayment(cashierName, externalTransactionId);
            fail("expected PaymentStateException");
        } catch (PaymentStateException e) {
            assertSame(PaymentState.Unknown, e.getState());
        }
    }

    @Test
    public void shouldUseDaoToFinishPayment() throws Exception {
        service.finishPayment(cashierName, externalTransactionId);
        verify(dao).updateState(cashierName, externalTransactionId, PaymentState.Finished);
    }

    @Test
    public void shouldThrowExceptionWithUnknownStateWhenFinishingAndDataSourceNotConnected() throws Exception {
        CannotGetJdbcConnectionException notConnectedException = new CannotGetJdbcConnectionException("Not Connected", new SQLException());
        doThrow(notConnectedException).when(dao).updateState(cashierName, externalTransactionId, PaymentState.Finished);
        try {
            service.finishPayment(cashierName, externalTransactionId);
            fail("expected PaymentStateException");
        } catch (PaymentStateException e) {
            assertSame(PaymentState.Unknown, e.getState());
        }
    }

    @Test
    public void shouldUseDaoToFailPayment() throws Exception {
        service.failPayment(cashierName, externalTransactionId);
        verify(dao).updateState(cashierName, externalTransactionId, PaymentState.Failed);
    }

    @Test
    public void shouldThrowExceptionWithUnknownStateWhenFailingAndDataSourceNotConnected() throws Exception {
        CannotGetJdbcConnectionException notConnectedException = new CannotGetJdbcConnectionException("Not Connected", new SQLException());
        doThrow(notConnectedException).when(dao).updateState(cashierName, externalTransactionId, PaymentState.Failed);
        try {
            service.failPayment(cashierName, externalTransactionId);
            fail("expected PaymentStateException");
        } catch (PaymentStateException e) {
            assertSame(PaymentState.Unknown, e.getState());
        }
    }

    @Test
    public void shouldUseDaoToFinishFailingPayment() throws Exception {
        service.failPayment(cashierName, externalTransactionId, false);
        verify(dao).updateState(cashierName, externalTransactionId, PaymentState.FinishedFailed);
    }

    @Test
    public void shouldThrowExceptionWithUnknownStateWhenFailingUnrepeatableTxnAndDataSourceNotConnected() throws Exception {
        CannotGetJdbcConnectionException notConnectedException = new CannotGetJdbcConnectionException("Not Connected", new SQLException());
        doThrow(notConnectedException).when(dao).updateState(cashierName, externalTransactionId, PaymentState.FinishedFailed);
        try {
            service.failPayment(cashierName, externalTransactionId, false);
            fail("expected PaymentStateException");
        } catch (PaymentStateException e) {
            assertSame(PaymentState.Unknown, e.getState());
        }
    }
}
