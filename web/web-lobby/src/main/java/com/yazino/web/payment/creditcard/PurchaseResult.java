package com.yazino.web.payment.creditcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class PurchaseResult implements Serializable {
    private static final long serialVersionUID = 4091801936919476393L;

    private final String merchant;
    private final PurchaseOutcome outcome;
    private final String customerEmail;
    private final String message;
    private final BigDecimal money;
    private final BigDecimal chips;
    private final Currency currency;
    private final String cardNumberObscured;
    private final String internalTransactionId;
    private final String externalTransactionId;
    private final String trace;

    public PurchaseResult(final PurchaseOutcome purchaseOutcome,
                          final String message,
                          final String emailAddress) {
        this("none", purchaseOutcome, emailAddress, message, Currency.getInstance("GBP"), BigDecimal.ZERO,
                BigDecimal.ZERO, null, null, null, null);
    }

    public PurchaseResult(final String merchant,
                          final PurchaseOutcome outcome,
                          final String customerEmail,
                          final String message,
                          final Currency currency,
                          final BigDecimal money,
                          final BigDecimal chips,
                          final String cardNumberObscured,
                          final String internalTransactionId,
                          final String externalTransactionId,
                          final String trace) {
        notBlank(merchant, "merchant is null/blank");
        notNull(outcome, "outcome is null");
        notNull(message, "message is null");
        notNull(currency, "currency is null");
        notNull(money, "money is null");
        notNull(chips, "chips is null");

        this.merchant = merchant;
        this.outcome = outcome;
        this.customerEmail = customerEmail;
        this.message = message;
        this.currency = currency;
        this.money = money;
        this.chips = chips;
        this.cardNumberObscured = cardNumberObscured;
        this.internalTransactionId = internalTransactionId;
        this.externalTransactionId = externalTransactionId;
        this.trace = trace;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getMessage() {
        return message;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public PurchaseOutcome getOutcome() {
        return outcome;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCardNumberObscured() {
        return cardNumberObscured;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public String getTrace() {
        return trace;
    }

    public Map<String, Object> buildArgumentMap() {
        final Map<String, Object> args = new HashMap<>();
        args.put("message", message);
        args.put("merchant", merchant);
        args.put("result", outcome.name());
        args.put("customerEmail", customerEmail);
        args.put("money", money);
        args.put("chips", chips);
        args.put("currencyCode", currency.getCurrencyCode());
        args.put("cardNumberObscured", cardNumberObscured);
        args.put("internalTransactionId", internalTransactionId);
        args.put("externalTransactionId", externalTransactionId);
        args.put("trace", trace);

        final DateTime dateTime = new DateTime();
        args.put("date", dateTime.toString("dd/MM/yyyy"));
        args.put("time", dateTime.toString("HH:mm:ss"));
        return args;
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
        final PurchaseResult rhs = (PurchaseResult) obj;
        return new EqualsBuilder()
                .append(outcome, rhs.outcome)
                .append(cardNumberObscured, rhs.cardNumberObscured)
                .append(chips, rhs.chips)
                .append(currency, rhs.currency)
                .append(customerEmail, rhs.customerEmail)
                .append(externalTransactionId, rhs.externalTransactionId)
                .append(internalTransactionId, rhs.internalTransactionId)
                .append(merchant, rhs.merchant)
                .append(message, rhs.message)
                .append(money, rhs.money)
                .append(trace, rhs.trace)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(outcome)
                .append(cardNumberObscured)
                .append(chips)
                .append(currency)
                .append(customerEmail)
                .append(externalTransactionId)
                .append(internalTransactionId)
                .append(merchant)
                .append(message)
                .append(money)
                .append(trace)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(outcome)
                .append(cardNumberObscured)
                .append(chips)
                .append(currency)
                .append(customerEmail)
                .append(externalTransactionId)
                .append(internalTransactionId)
                .append(merchant)
                .append(message)
                .append(money)
                .append(trace)
                .toString();
    }
}
