package com.yazino.web.payment.creditcard;

import org.apache.commons.lang3.StringUtils;

import static com.yazino.web.payment.CustomerData.obscureMiddleCardNumbers;

public class CreditCardFormBuilder {
    private String paymentOptionId;
    private Long promotionId;
    private String creditCardNumber;
    private String cvc2;
    private String expirationMonth;
    private String expirationYear;
    private String cardHolderName;
    private String cardId;
    private String emailAddress;
    private String termsAndServiceAgreement;
    private String gameType;
    private String obscuredCardNumber;

    public static CreditCardFormBuilder valueOf() {
        return new CreditCardFormBuilder();
    }

    public static CreditCardFormBuilder from(final CreditCardForm form) {
        return new CreditCardFormBuilder()
                .withPaymentOptionId(form.getPaymentOptionId())
                .withCardId(form.getCardId())
                .withCreditCardNumber(form.getCreditCardNumber())
                .withCvc2(form.getCvc2())
                .withExpirationMonth(form.getExpirationMonth())
                .withExpirationYear(form.getExpirationYear())
                .withCardHolderName(form.getCardHolderName())
                .withCardId(form.getCardId())
                .withEmailAddress(form.getEmailAddress())
                .withTermsAndServiceAgreement(form.getTermsAndServiceAgreement());
    }

    private CreditCardFormBuilder() {
    }

    public CreditCardFormBuilder withPaymentOptionId(final String paymentOptionId) {
        this.paymentOptionId = paymentOptionId;
        return this;
    }

    public CreditCardFormBuilder withPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
        return this;
    }

    public CreditCardFormBuilder withCreditCardNumber(final String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
        return this;
    }

    public CreditCardFormBuilder withCvc2(final String cvc2) {
        this.cvc2 = cvc2;
        return this;
    }

    public CreditCardFormBuilder withExpirationMonth(final String expirationMonth) {
        this.expirationMonth = expirationMonth;
        return this;
    }

    public CreditCardFormBuilder withExpirationYear(final String expirationYear) {
        this.expirationYear = expirationYear;
        return this;
    }

    public CreditCardFormBuilder withCardHolderName(final String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    public CreditCardFormBuilder withCardId(final String cardId) {
        this.cardId = cardId;
        return this;
    }

    public CreditCardFormBuilder withEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public CreditCardFormBuilder withTermsAndServiceAgreement(final String termsAndServiceAgreement) {
        this.termsAndServiceAgreement = termsAndServiceAgreement;
        return this;
    }

    public CreditCardFormBuilder withGameType(final String gameType) {
        this.gameType = gameType;
        return this;
    }

    public CreditCardFormBuilder withObscuredCardNumber(final String obscuredCardNumber) {
        this.obscuredCardNumber = obscuredCardNumber;
        return this;
    }

    public CreditCardForm build() {
        if (StringUtils.isBlank(obscuredCardNumber) && StringUtils.isNotBlank(creditCardNumber)) {
            this.obscuredCardNumber = obscureMiddleCardNumbers(creditCardNumber);
        }
        return new CreditCardForm(paymentOptionId,
                promotionId,
                creditCardNumber,
                cvc2,
                expirationMonth,
                expirationYear,
                cardHolderName,
                cardId,
                emailAddress,
                termsAndServiceAgreement,
                gameType,
                obscuredCardNumber);
    }
}