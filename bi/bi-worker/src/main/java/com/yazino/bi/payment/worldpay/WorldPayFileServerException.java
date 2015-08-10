package com.yazino.bi.payment.worldpay;

import java.io.IOException;

public class WorldPayFileServerException extends IOException {
    private static final long serialVersionUID = 4757230750838366836L;

    public WorldPayFileServerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
