package com.yazino.web.payment.creditcard;

import java.util.ArrayList;
import java.util.List;

public class CreditCardFormBuilderForTest {
    private String paymentOptionId = "option2";
    private String creditCardNumber = "4200000000000000";
    private String cvc2 = "123";
    private String expirationMonth ="11";
    private String expirationYear = "2019";
    private String cardHolderName = "Nick Jones";
    private String emailAddress = "somebody@somewhere.com";
    private String termsAndServiceAgreement = "true";
    private Long promotionId = 1l;

    private List<String> invalidFields = new ArrayList<String>();
    private List<String> missingFields = new ArrayList<String>();

    public CreditCardForm build() {
        return CreditCardFormBuilder.valueOf()
                .withPaymentOptionId(paymentOptionId)
                .withPromotionId(promotionId)
                .withCreditCardNumber(creditCardNumber)
                .withCvc2(cvc2)
                .withExpirationMonth(expirationMonth)
                .withExpirationYear(expirationYear)
                .withCardHolderName(cardHolderName)
                .withEmailAddress(emailAddress)
                .withTermsAndServiceAgreement(termsAndServiceAgreement)
                .build();
    }

    public CreditCardFormBuilderForTest withCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
        return this;

    }

    public CreditCardFormBuilderForTest withEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public CreditCardFormBuilderForTest withCvc2(String cvc2) {
        this.cvc2 = cvc2;
        return this;
    }

    public CreditCardFormBuilderForTest withCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    public CreditCardFormBuilderForTest withExpiryYear(String expirationYear) {
        this.expirationYear = expirationYear;
        return this;
    }
}
