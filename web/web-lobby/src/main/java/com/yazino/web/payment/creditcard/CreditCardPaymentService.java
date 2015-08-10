package com.yazino.web.payment.creditcard;

public interface CreditCardPaymentService {

    PurchaseResult purchase(PurchaseRequest purchaseRequest);

    boolean accepts(PurchaseRequest purchaseRequest);

}
