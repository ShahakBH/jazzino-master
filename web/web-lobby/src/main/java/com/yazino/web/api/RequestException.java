package com.yazino.web.api;

public class RequestException extends Exception {
    private final int httpStatusCode;
    private final String error;

    public RequestException(int httpStatusCode, String error) {
        this.httpStatusCode = httpStatusCode;
        this.error = error;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getError() {
        return error;
    }
}
