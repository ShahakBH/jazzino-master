package com.yazino.web.domain;

public enum PaymentEmailBodyTemplate {
    Facebook("using Facebook. "),
    CreditCard("with card %s. "),
    Paypal("using Paypal. Paypal will send you a receipt of purchase shortly. "),
    Zong("using your mobile! "),
    iTunes("using iTunes. iTunes will send you a receipt of purchase shortly. "),
    GoogleCheckout("using Google Play. Google will send you a receipt of purchase shortly. "),
    Amazon("using Amazon. Amazon will send you a receipt of purchase shortly. ");

    private final String bodyTemplate;

    private PaymentEmailBodyTemplate(final String bodyTemplate) {

        this.bodyTemplate = bodyTemplate;
    }

    public String getBody(final String cardNumber) {
        if (this == CreditCard) {
            return String.format(bodyTemplate, cardNumber);
        }

        return bodyTemplate;
    }
}
