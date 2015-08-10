package com.yazino.platform.payment.android;

import java.math.BigDecimal;

public interface AndroidPaymentStateService {
    void createPurchaseRequest(BigDecimal playerId,
                               String gameType,
                               String internalTransactionId,
                               String productId,
                               Long promoId) throws AndroidPaymentStateException;

    void createCreditPurchaseLock(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException;

    void markPurchaseAsCredited(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException;

    /**
     * Updates the payment from CREDITING to CANCELLED.
     * @param playerId
     * @param internalTransactionId
     * @throws AndroidPaymentStateException if current state is not CREDITING or persistence fails
     */
    void markPurchaseAsCancelled(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException;

    /**
     * Updates the payment from CREATED to CANCELLED.
     * @param playerId
     * @param internalTransactionId
     * @throws AndroidPaymentStateException  if current state is not CREATED or persistence fails
     */
    void markPurchaseAsUserCancelled(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException;

    void markPurchaseAsFailed(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException;

    AndroidPaymentStateDetails findPaymentStateDetailsFor(String internalTransactionId);

}
