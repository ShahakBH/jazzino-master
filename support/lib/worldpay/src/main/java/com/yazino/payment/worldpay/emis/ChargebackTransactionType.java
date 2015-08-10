package com.yazino.payment.worldpay.emis;

import static org.apache.commons.lang3.Validate.notNull;

public enum ChargebackTransactionType {
    SALE("D"),
    REFUND("C");

    private final String code;

    private ChargebackTransactionType(final String code) {
        notNull(code, "code may not be null");

        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChargebackTransactionType fromCode(final String code) {
        for (ChargebackTransactionType transactionType : values()) {
            if (transactionType.getCode().equals(code)) {
                return transactionType;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
