package com.yazino.payment.worldpay.fx;

import java.io.IOException;

public class ParseException extends IOException {
    private static final long serialVersionUID = -6792874887102483822L;

    public ParseException(final String formatString, final Object... args) {
        super(String.format(formatString, args));
    }
}
