package com.yazino.platform.payment;

public interface PaymentStateService {
    void startPayment(String cashierName,
                      String externalTransactionId) throws PaymentStateException;

    void finishPayment(String cashierName,
                       String externalTransactionId) throws PaymentStateException;

    void failPayment(String cashierName,
                     String externalTransactionId) throws PaymentStateException;

    void failPayment(String cashierName,
                     String externalTransactionId,
                     boolean allowRetries) throws PaymentStateException;
}
