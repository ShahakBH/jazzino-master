package com.yazino.payment.worldpay.nvp;

import java.util.regex.Pattern;

public enum NVPType {
    ALPHANUMERIC("^.+$"),
    ALPHA("^[^0-9]+$"),
    NUMERIC("^[0-9.]+$");

    private final Pattern validationRegex;

    private NVPType(final String validationRegex) {
        if (validationRegex != null) {
            this.validationRegex = Pattern.compile(validationRegex);
        } else {
            this.validationRegex = null;
        }
    }

    public boolean validate(final String value) {
        return validationRegex == null || validationRegex.matcher(value).matches();
    }
}
