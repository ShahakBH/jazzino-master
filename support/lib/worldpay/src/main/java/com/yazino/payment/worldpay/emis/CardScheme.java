package com.yazino.payment.worldpay.emis;

import static org.apache.commons.lang3.Validate.notNull;

public enum CardScheme {
    MASTERCARD("MSC"),
    MASTERCARD_DEBIT("DMC"),
    VISA("VIS"),
    VISA_DEBIT("DEL");

    private final String code;

    private CardScheme(final String code) {
        notNull(code, "code may not be null");

        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static CardScheme fromCode(final String code) {
        for (CardScheme cardScheme : values()) {
            if (cardScheme.getCode().equals(code)) {
                return cardScheme;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
