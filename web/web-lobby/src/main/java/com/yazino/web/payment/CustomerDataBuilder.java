package com.yazino.web.payment;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Currency;

import static com.yazino.web.payment.CustomerData.obscureMiddleCardNumbers;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CustomerDataBuilder {
    private BigDecimal amount;
    private Currency currency;
    private String transactionCountry;
    private String creditCardNumber;
    private String cvc2;
    private String expirationMonth;
    private String expirationYear;
    private String cardHolderName;
    private String cardId;
    private InetAddress customerIPAddress;
    private String emailAddress;
    private String gameType;
    private String obscureMiddleCardNumbers;

    public CustomerDataBuilder withAmount(final BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public CustomerDataBuilder withCurrency(final Currency currency) {
        this.currency = currency;
        return this;
    }

    public CustomerDataBuilder withTransactionCountry(final String transactionCountry) {
        this.transactionCountry = transactionCountry;
        return this;
    }

    public CustomerDataBuilder withCreditCardNumber(final String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
        return this;
    }

    public CustomerDataBuilder withCvc2(final String cvc2) {
        this.cvc2 = cvc2;
        return this;
    }

    public CustomerDataBuilder withExpirationMonth(final String expirationMonth) {
        this.expirationMonth = expirationMonth;
        return this;
    }

    public CustomerDataBuilder withExpirationYear(final String expirationYear) {
        this.expirationYear = expirationYear;
        return this;
    }

    public CustomerDataBuilder withCardHolderName(final String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    public CustomerDataBuilder withCardId(final String cardId) {
        this.cardId = cardId;
        return this;
    }

    public CustomerDataBuilder withCustomerIPAddress(final InetAddress customerIPAddress) {
        this.customerIPAddress = customerIPAddress;
        return this;
    }

    public CustomerDataBuilder withEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public CustomerDataBuilder withGameType(final String gameType) {
        this.gameType = gameType;
        return this;
    }

    public CustomerDataBuilder withObscureMiddleCardNumbers(final String obscureMiddleCardNumbers) {
        this.obscureMiddleCardNumbers = obscureMiddleCardNumbers;
        return this;
    }

    public CustomerData build() {
        String defaultObscuredCardNumbers = obscureMiddleCardNumbers;
        if (isBlank(defaultObscuredCardNumbers) && isNotBlank(creditCardNumber)) {
            defaultObscuredCardNumbers = obscureMiddleCardNumbers(creditCardNumber);
        }
        return new CustomerData(amount,
                currency,
                transactionCountry,
                creditCardNumber,
                cvc2,
                expirationMonth,
                expirationYear,
                cardHolderName,
                cardId,
                customerIPAddress,
                emailAddress,
                gameType,
                defaultObscuredCardNumbers);
    }


}