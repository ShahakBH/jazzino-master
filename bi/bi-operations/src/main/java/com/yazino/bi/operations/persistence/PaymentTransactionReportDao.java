package com.yazino.bi.operations.persistence;

import org.joda.time.DateTime;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.util.List;

/**
 * DAO supporting the payment transaction report information
 */
public interface PaymentTransactionReportDao {

    String SUCCESSFUL_STATUS = "SUCCESSFUL";
    String PURCHASE_TRANSACTIONS = "PURCHASES";
    String OFFER_TRANSACTIONS = "OFFERS";

    List<PaymentTransactionData> getPaymentTransactionData(DateTime fromDate,
                                                           DateTime toDate,
                                                           String currencyCode,
                                                           String cashier,
                                                           String txnStatus);

    List<PaymentTransactionData> getPaymentTransactionData(String transactionId);

    List<String> getAvailablePaymentMethods();
}
