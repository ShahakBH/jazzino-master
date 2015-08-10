package com.yazino.platform.payment;

import com.yazino.platform.model.PagedData;

import java.math.BigDecimal;

public interface PaymentService {

    PagedData<PendingSettlement> findAuthorised(int page, int pageSize);

    int cancelAllSettlementsForPlayer(BigDecimal playerId);

    void disputePayment(PaymentDispute paymentDispute);

    void resolveDispute(String internalTransactionId,
                        DisputeResolution resolution,
                        String resolvedBy,
                        String note);

    PagedData<DisputeSummary> findOpenDisputes(int page, int pageSize);

}
