package com.yazino.platform.payment.settlement;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.model.account.PaymentSettlement;

public interface PaymentSettlementProcessor {

    ExternalTransaction settle(final PaymentSettlement paymentSettlement);

    ExternalTransaction cancel(final PaymentSettlement paymentSettlement);

}
