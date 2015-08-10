package com.yazino.web.payment.creditcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CreditCardDetails {
    private final String creditCardNumber;
    private final String cvc2;
    private final String expirationMonth;
    private final String expirationYear;
    private final String cardHolderName;
    private final String cardId;
    private String obscuredCardNumber;

    public CreditCardDetails(final String creditCardNumber,
                             final String cvc2,
                             final String expirationMonth,
                             final String expirationYear,
                             final String cardHolderName,
                             final String cardId,
                             final String obscuredCardNumber) {
        this.creditCardNumber = creditCardNumber;
        this.cvc2 = cvc2;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cardHolderName = cardHolderName;
        this.cardId = cardId;
        this.obscuredCardNumber = obscuredCardNumber;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public String getCvc2() {
        return cvc2;
    }

    public String getExpirationMonth() {
        return expirationMonth;
    }

    public String getExpirationYear() {
        return expirationYear;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getCardId() {
        return cardId;
    }

    public String getObscuredCardNumber() {
        return obscuredCardNumber;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final CreditCardDetails rhs = (CreditCardDetails) obj;
        return new EqualsBuilder()
                .append(creditCardNumber, rhs.creditCardNumber)
                .append(cvc2, rhs.cvc2)
                .append(expirationMonth, rhs.expirationMonth)
                .append(expirationYear, rhs.expirationYear)
                .append(cardHolderName, rhs.cardHolderName)
                .append(cardId, rhs.cardId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(creditCardNumber)
                .append(cvc2)
                .append(expirationMonth)
                .append(expirationYear)
                .append(cardHolderName)
                .append(cardId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(creditCardNumber)
                .append(cvc2)
                .append(expirationMonth)
                .append(expirationYear)
                .append(cardHolderName)
                .append(cardId)
                .toString();
    }

}
