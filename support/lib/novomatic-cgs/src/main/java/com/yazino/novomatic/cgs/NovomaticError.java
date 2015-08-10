package com.yazino.novomatic.cgs;

public class NovomaticError extends Exception {
    public static final String TYPE = "rsp_gmengine_error";
    private final Long errorCode;

    public NovomaticError(Long errorCode, String description) {
        super(description);
        this.errorCode = errorCode;
    }

    public Long getErrorCode() {
        return errorCode;
    }
}
