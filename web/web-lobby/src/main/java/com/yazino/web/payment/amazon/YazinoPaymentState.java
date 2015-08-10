package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;

public interface YazinoPaymentState {

    boolean startPayment(final PaymentContext paymentContext, final VerifiedOrder verifiedOrder, String cashierName);

    boolean recordPayment(final PaymentContext paymentContext, final VerifiedOrder order, String cashierName, final Purchase purchaseRequest);

    void finishPayment(final PaymentContext paymentContext, final VerifiedOrder verifiedOrder, String cashierName);

    void logFailure(PaymentContext paymentContext,
                    VerifiedOrder order,
                    String cashierName,
                    Platform platform,
                    String reasonForFailure,
                    final Purchase purchaseRequest);
}
