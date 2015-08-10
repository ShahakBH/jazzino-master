package com.yazino.platform.payment.dispute;

import com.yazino.platform.payment.PaymentDispute;

public interface PaymentDisputeProcessor {

    void raise(PaymentDispute dispute);

    void resolve(PaymentDispute dispute);

}
