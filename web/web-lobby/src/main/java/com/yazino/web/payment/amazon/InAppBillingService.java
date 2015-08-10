package com.yazino.web.payment.amazon;

import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;

public interface InAppBillingService {

    Purchase creditPurchase(final PaymentContext paymentContext, final String userId,
                            final String purchaseToken, final String productId, final String orderId);

    void logFailedTransaction(final PaymentContext paymentContext,
                              final String productId, final String orderId, final String reasonForFailure);
}
