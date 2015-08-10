package com.yazino.web.payment;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Currency;

public final class CustomerData implements Serializable {
    private static final long serialVersionUID = -5780385504941125515L;
    private static final int LAST_HIDDEN_CHAR = 12;
    private static final int FIRST_HIDDEN_CHAR = 4;

    private final BigDecimal amount;
    private final Currency currency;
    private final String transactionCountryISO3166;
    private final String creditCardNumber;
    private final String obscureMiddleCardNumbers;
    private final String cvc2;
    private final String expirationMonth;
    private final String expirationYear;
    private final String cardHolderName;
    private final String cardId;
    private final InetAddress customerIPAddress;
    private final String emailAddress;
    private final String gameType;

    CustomerData(final BigDecimal amount,
                        final Currency currency,
                        final String transactionCountry,
                        final String creditCardNumber,
                        final String cvc2,
                        final String expirationMonth,
                        final String expirationYear,
                        final String cardHolderName,
                        final String cardId,
                        final InetAddress customerIPAddress,
                        final String emailAddress,
                        final String gameType,
                        final String obscureMiddleCardNumbers) {
        this.amount = amount;
        this.currency = currency;
        this.transactionCountryISO3166 = transactionCountry;
        this.creditCardNumber = creditCardNumber;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
        this.cardHolderName = cardHolderName;
        this.cardId = cardId;
        this.customerIPAddress = customerIPAddress;
        this.cvc2 = cvc2;
        this.emailAddress = emailAddress;
        this.gameType = gameType;
        this.obscureMiddleCardNumbers = obscureMiddleCardNumbers;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getTransactionCountryISO3166() {
        return transactionCountryISO3166;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public String getCardId() {
        return cardId;
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

    public InetAddress getCustomerIPAddress() {
        return customerIPAddress;
    }

    public String getCvc2() {
        return cvc2;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getGameType() {
        return gameType;
    }

    public String getObscureMiddleCardNumbers() {
        return obscureMiddleCardNumbers;
    }

    public static String obscureMiddleCardNumbers(String creditCardNumber) {
        if (creditCardNumber == null) {
            return null;
        }

        final char[] cardNumberArray = creditCardNumber.toCharArray();
        if (cardNumberArray.length >= LAST_HIDDEN_CHAR) {
            for (int count = FIRST_HIDDEN_CHAR; count < LAST_HIDDEN_CHAR; count++) {
                cardNumberArray[count] = 'X';
            }
        }
        return new String(cardNumberArray);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final CustomerData rhs = (CustomerData) obj;
        return new EqualsBuilder()
                .append(amount, rhs.amount)
                .append(currency, rhs.currency)
                .append(transactionCountryISO3166, rhs.transactionCountryISO3166)
                .append(creditCardNumber, rhs.creditCardNumber)
                .append(cvc2, rhs.cvc2)
                .append(expirationMonth, rhs.expirationMonth)
                .append(expirationYear, rhs.expirationYear)
                .append(cardHolderName, rhs.cardHolderName)
                .append(customerIPAddress, rhs.customerIPAddress)
                .append(emailAddress, rhs.emailAddress)
                .append(gameType, rhs.gameType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 731)
                .append(amount)
                .append(currency)
                .append(transactionCountryISO3166)
                .append(creditCardNumber)
                .append(cvc2)
                .append(expirationMonth)
                .append(expirationYear)
                .append(cardHolderName)
                .append(customerIPAddress)
                .append(emailAddress)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("amount", amount)
                .append("currency", currency)
                .append("transactionCountry", transactionCountryISO3166)
                .append("creditCardNumber", obscureMiddleCardNumbers)
                .append("cvc2", "***")
                .append("expirationMonth", expirationMonth)
                .append("expirationYear", expirationYear)
                .append("cardHolderName", cardHolderName)
                .append("cardId", cardId)
                .append("customerIPAddress", customerIPAddress)
                .append("emailAddress", emailAddress)
                .append("gameType", gameType)
                .toString();
    }
}
