package com.yazino.web.payment.creditcard;

public class CreditCardDetailsBuilder {

    private String creditCardNumber;
    private String cvc2;
    private String expirationMonth;
    private String expirationYear;
    private String cardHolderName;
    private String cardId;
    private String obscuredCardNumber;

    public static CreditCardDetailsBuilder builder() {
        return new CreditCardDetailsBuilder();
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public CreditCardDetailsBuilder withCreditCardNumber(final String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
        return this;
    }

    public String getCvc2() {
        return cvc2;
    }

    public CreditCardDetailsBuilder withCvc2(final String cvc2) {
        this.cvc2 = cvc2;
        return this;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public CreditCardDetailsBuilder withExpirationMonth(final String expirationMonth) {
        this.expirationMonth = expirationMonth;
        return this;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public CreditCardDetailsBuilder withExpirationYear(final String expirationYear) {
        this.expirationYear = expirationYear;
        return this;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public CreditCardDetailsBuilder withCardHolderName(final String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    public String getCardId() {
        return cardId;
    }

    public CreditCardDetailsBuilder withCardId(final String cardId) {
        this.cardId = cardId;
        return this;
    }

    public String getObscuredCardNumber() {
        return obscuredCardNumber;
    }

    public CreditCardDetailsBuilder withObscuredCardNumber(final String obscuredCardNumber) {
        this.obscuredCardNumber = obscuredCardNumber;
        return this;
    }

    public CreditCardDetails build() {
        return new CreditCardDetails(creditCardNumber,
                                     cvc2,
                                     expirationMonth,
                                     expirationYear,
                                     cardHolderName,
                                     cardId,
                                     obscuredCardNumber);
    }

}
