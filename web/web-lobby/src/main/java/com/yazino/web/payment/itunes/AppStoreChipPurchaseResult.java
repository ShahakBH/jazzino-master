package com.yazino.web.payment.itunes;

import java.math.BigDecimal;

class AppStoreChipPurchaseResult {
    @Deprecated private String error; // use message instead
    private boolean success;
    private BigDecimal chipAmount;
    private BigDecimal cashAmount;
    private String currencyCode;
    private String transactionIdentifier;
    private String message;

    public String getTransactionIdentifier() {
        return transactionIdentifier;
    }

    public void setTransactionIdentifier(final String transactionIdentifier) {
        this.transactionIdentifier = transactionIdentifier;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public BigDecimal getChipAmount() {
        return chipAmount;
    }

    public void setChipAmount(final BigDecimal chipAmount) {
        this.chipAmount = chipAmount;
    }

    public BigDecimal getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(final BigDecimal cashAmount) {
        this.cashAmount = cashAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currency) {
        this.currencyCode = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}
