package com.yazino.web.payment.amazon;

import java.util.HashMap;
import java.util.Map;

public enum VerificationResult {
    VALID(200),
    INVALID_TRANSACTION(400),
    INVALID_USER(497),
    INVALID_TOKEN(498),
    TOKEN_EXPIRED(499);

    private static final Map<Integer, VerificationResult> STATUS_CODES = new HashMap<>();

    static {
        for (VerificationResult verificationResult : values()) {
            STATUS_CODES.put(verificationResult.statusCode, verificationResult);
        }
    }

    private final int statusCode;

    VerificationResult(int statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isValid() {
        return this.statusCode == 200;
    }

    public static VerificationResult fromStatusCode(int statusCode) {
        if (!STATUS_CODES.containsKey(statusCode)) {
            throw new IllegalArgumentException("Unknown verification status code " + statusCode);
        }
        return STATUS_CODES.get(statusCode);
    }
}
