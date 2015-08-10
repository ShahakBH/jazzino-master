package com.yazino.web.domain.payment;

public class CardRegistrationResultBuilder {
    private String cardId;
    private String customerId;
    private String obscuredCardNumber;
    private String customerName;
    private String expiryDate;
    private String messageCode;
    private String message;

    public String getCardId() {
        return cardId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getObscuredCardNumber() {
        return obscuredCardNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public String getMessage() {
        return message;
    }

    public static CardRegistrationResultBuilder builder() {
        return new CardRegistrationResultBuilder();
    }

    public CardRegistrationResultBuilder withCardId(final String cardId) {
        this.cardId = cardId;
        return this;
    }

    public CardRegistrationResultBuilder withCustomerId(final String customerId) {
        this.customerId = customerId;
        return this;
    }

    public CardRegistrationResultBuilder withObscuredCardNumber(final String obscuredCardNumber) {
        this.obscuredCardNumber = obscuredCardNumber;
        return this;
    }

    public CardRegistrationResultBuilder withAccountName(final String customerName) {
        this.customerName = customerName;
        return this;
    }

    public CardRegistrationResultBuilder withExpiryDate(final String expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public CardRegistrationResultBuilder withMessageCode(final String messageCode) {
        this.messageCode = messageCode;
        return this;
    }

    public CardRegistrationResultBuilder withMessage(final String message) {
        this.message = message;
        return this;
    }

    public CardRegistrationResult build() {
        return new CardRegistrationResult(cardId,
                customerId,
                obscuredCardNumber,
                customerName,
                expiryDate,
                messageCode,
                message);
    }


}
