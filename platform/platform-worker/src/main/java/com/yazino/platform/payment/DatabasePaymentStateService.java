package com.yazino.platform.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang.Validate.notNull;

@Service("paymentStateService")
public class DatabasePaymentStateService implements PaymentStateService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabasePaymentStateService.class);

    private final JDBCPaymentStateDao dao;

    @Autowired
    public DatabasePaymentStateService(final JDBCPaymentStateDao dao) {
        notNull(dao);
        this.dao = dao;
    }

    /**
     * Start the payment process.
     *
     * @param cashierName,           never null
     * @param externalTransactionId, never null
     * @throws com.yazino.platform.payment.PaymentStateException should the payment not be startable
     */
    @Override
    public void startPayment(final String cashierName,
                             final String externalTransactionId) throws PaymentStateException {
        try {
            final int rows = dao.insertState(cashierName, externalTransactionId, PaymentState.Started);
            LOG.debug("Started payment for cashier {}, externalTransactionId {} yielded {} rows updated",
                    cashierName, externalTransactionId, rows);
        } catch (DataIntegrityViolationException e) {
            LOG.debug("Attempt to start a started payment for cashier {}, externalTransactionId {}",
                    cashierName, externalTransactionId);
            final PaymentState existingState = attemptRead(cashierName, externalTransactionId);
            throw new PaymentStateException(existingState);
        } catch (DataAccessException e) {
            LOG.warn("Failed to start payment", e);
            throw new PaymentStateException(PaymentState.Unknown);
        }
    }

    /**
     * Finish this payment transaction.
     *
     * @param cashierName,           never null
     * @param externalTransactionId, never null
     * @throws PaymentStateException should the payment not be finishable
     */
    @Override
    public void finishPayment(final String cashierName,
                              final String externalTransactionId) throws PaymentStateException {
        try {
            final int rows = dao.updateState(cashierName, externalTransactionId, PaymentState.Finished);
            LOG.debug("Finishing payment with cashier {}, externalTransactionId {} yielded {} rows updated",
                    cashierName, externalTransactionId, rows);
        } catch (DataAccessException e) {
            LOG.warn("Failed to finish payment", e);
            throw new PaymentStateException(PaymentState.Unknown);
        }
    }

    /**
     * Manually fail this transaction.
     *
     * @param cashierName,           never null
     * @param externalTransactionId, never null
     * @throws PaymentStateException should the payment not be failable
     */
    @Override
    public void failPayment(final String cashierName,
                            final String externalTransactionId) throws PaymentStateException {
        try {
            final int rows = dao.updateState(cashierName, externalTransactionId, PaymentState.Failed);
            LOG.debug("Failing payment with cashier {}, externalTransactionId {} yielded {} rows updated",
                    new Object[]{cashierName, externalTransactionId, rows});
        } catch (DataAccessException e) {
            LOG.warn("Failed to finish payment", e);
            throw new PaymentStateException(PaymentState.Unknown);
        }
    }

    /**
     * Fail the transaction. The transaction can be retried depending on value of {@code allowRetries}
     *
     * @param cashierName           never null
     * @param externalTransactionId never null
     * @param allowRetries          if true, then the transaction is failed with state {@code PaymentState.Failed}, if
     *                              false then {@code PaymentState.FinishedFailed} is used.
     * @throws PaymentStateException should the payment state  not be failable.
     */
    @Override
    public void failPayment(String cashierName, String externalTransactionId, boolean allowRetries) throws PaymentStateException {
        if (allowRetries == true) {
            failPayment(cashierName, externalTransactionId);
        } else {
            try {
                final int rows = dao.updateState(cashierName, externalTransactionId, PaymentState.FinishedFailed);
                LOG.debug("Finishing failed payment with cashier {}, externalTransactionId {} yielded {} rows updated",
                        new Object[]{cashierName, externalTransactionId, rows});
            } catch (DataAccessException e) {
                LOG.warn("Failed to finish failing payment", e);
                throw new PaymentStateException(PaymentState.Unknown);
            }
        }
    }

    private PaymentState attemptRead(final String cashierName,
                                     final String externalTransactionId) {
        try {
            return dao.readState(cashierName, externalTransactionId);
        } catch (DataAccessException e) {
            LOG.warn("Failed to read payment state for cashier {}, externalTransactionId {} due to {}",
                    new Object[]{cashierName, externalTransactionId, e.getMessage()});
        }
        return PaymentState.Unknown;
    }


}
