package com.yazino.web.domain;

public class CashierConfiguration {
    private String cashierId;
    private String cashierUrl;
    private Boolean isEnabled = true;

    public String getCashierId() {
        return cashierId;
    }

    public String getCashierUrl() {
        return cashierUrl;
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public void setCashierId(final String cashierId) {
        this.cashierId = cashierId;
    }

    public void setCashierUrl(final String cashierUrl) {
        this.cashierUrl = cashierUrl;
    }

    public void setEnabled(final Boolean enabled) {
        isEnabled = enabled;
    }
}
