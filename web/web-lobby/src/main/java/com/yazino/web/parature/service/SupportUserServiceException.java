package com.yazino.web.parature.service;


public class SupportUserServiceException extends Exception {
    private static final long serialVersionUID = 2062452020869373310L;

    public SupportUserServiceException(final String message) {
        super(message);
    }

    public SupportUserServiceException(final String message, final Throwable nested) {
        super(message, nested);
    }
}
