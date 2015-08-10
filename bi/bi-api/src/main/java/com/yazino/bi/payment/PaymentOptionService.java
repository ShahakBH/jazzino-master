package com.yazino.bi.payment;

import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PaymentOptionService {

    Map<Currency, List<PaymentOption>> getAllDefaults(Platform platform);

    PaymentOption getDefault(String paymentOptionId, Platform platform);

    Collection<PaymentOption> getAllPaymentOptions(Platform platform);

    Collection<PaymentOption> getAllPaymentOptions(Currency currency,
                                                   Platform platform);

}
