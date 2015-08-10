package com.yazino.platform.account;

public enum ExternalTransactionStatus {
    REQUEST(false),
    AUTHORISED(true),
    SETTLED(false),
    SUCCESS(true),
    FAILURE(false),
    ERROR(false),
    CANCELLED(false);

    private final boolean postRequired;

    private ExternalTransactionStatus(final boolean postRequired) {
        this.postRequired = postRequired;
    }

    public boolean isPostRequired() {
        return postRequired;
    }
}
