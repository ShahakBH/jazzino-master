package com.yazino.web.domain.payment;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class RegisteredCreditCardDetailsBuilder {
    private static final int EXPECTED_DATE_LENGTH = 6;
    public static final String DEFAULT_CARD = "1";
    private String cardId;
    private String obscuredNumber;
    private String accountName;
    private String expiryDate;
    private String issueCountry;
    private String cardIssuer;
    private String creditCardType;
    private String isDefault;

    public static RegisteredCreditCardDetailsBuilder valueOf() {
        return new RegisteredCreditCardDetailsBuilder();
    }

    public RegisteredCreditCardDetailsBuilder withCardId(final String cardId) {
        this.cardId = cardId;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withObscuredNumber(final String obscuredNumber) {
        this.obscuredNumber = obscuredNumber;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withAccountName(final String accountName) {
        this.accountName = accountName;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withExpiryDate(final String expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withIssueCountry(final String issueCountry) {
        this.issueCountry = issueCountry;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withCardIssuer(final String cardIssuer) {
        this.cardIssuer = cardIssuer;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withCreditCardType(final String creditCardType) {
        this.creditCardType = creditCardType;
        return this;
    }

    public RegisteredCreditCardDetailsBuilder withIsDefault(final String isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public RegisteredCreditCardDetails build() {
        boolean cardIsDefault = isNotBlank(isDefault) && isDefault.equals(DEFAULT_CARD);
        return new RegisteredCreditCardDetails(
                cardId,
                obscuredNumber,
                accountName,
                cardIssuer,
                extractExpiryMonth(expiryDate),
                extractExpiryYear(expiryDate),
                "***",
                issueCountry,
                creditCardType,
                cardIsDefault
        );
    }

    private static String extractExpiryYear(String expiryDate) {
        if (isNotBlank(expiryDate) && expiryDate.length() == EXPECTED_DATE_LENGTH) {
            return expiryDate.substring(2);
        }
        return null;
    }

    private static String extractExpiryMonth(String expiryDate) {
        if (isNotBlank(expiryDate) && expiryDate.length() == EXPECTED_DATE_LENGTH) {
            return expiryDate.substring(0, 2);
        }
        return null;
    }
}
