package com.yazino.web.payment.paypalec;

public class PaypalRequestException extends Exception {
    private static final long serialVersionUID = 8095534151082338109L;

    public PaypalRequestException() {
    }

    public PaypalRequestException(final String msg) {
        super(msg);
    }

    public PaypalRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PaypalRequestException(final Throwable cause) {
        super(cause);
    }
}

