package com.yazino.web.domain.payment;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class RegisteredCreditCardDetails {
    private final String cardId;
    private final String obscuredNumber;
    private final String accountName;
    private final String cardIssuer;
    private final String expiryMonth;
    private final String expiryYear;
    private final String cvc2;
    private final String issueCountry;
    private final String creditCardType;
    private final boolean isDefault;

    public String getCardId() {
        return cardId;
    }

    public String getObscuredNumber() {
        return obscuredNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getCardIssuer() {
        return cardIssuer;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public String getCvc2() {
        return cvc2;
    }

    public String getIssueCountry() {
        return issueCountry;
    }

    public String getCreditCardType() {
        return creditCardType;
    }

    public boolean isDefault() {
        return isDefault;
    }

    RegisteredCreditCardDetails(final String cardId,
                                final String obscuredNumber,
                                final String accountName,
                                final String cardIssuer,
                                final String expiryMonth,
                                final String expiryYear,
                                final String cvc2,
                                final String issueCountry,
                                final String creditCardType,
                                final boolean isDefault) {

        this.cardId = cardId;
        this.obscuredNumber = obscuredNumber;
        this.accountName = accountName;
        this.cardIssuer = cardIssuer;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvc2 = cvc2;
        this.issueCountry = issueCountry;
        this.creditCardType = creditCardType;
        this.isDefault = isDefault;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
