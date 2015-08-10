package com.yazino.web.payment;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.community.PaymentPreferences;

/**
 * Transforms a Yazino payment option id into something payment provider specific.
 */
public interface PaymentOptionIdTransformer {

    /**
     * Transform the PaymentOption is into something else...
     *
     * @param gameType
     * @param paymentOption
     * @param paymentMethod
     * @return transformed id or null if option id couldn't be transformed.
     */
    String transformPaymentOptionId(String gameType, PaymentOption paymentOption, PaymentPreferences.PaymentMethod paymentMethod);
}
