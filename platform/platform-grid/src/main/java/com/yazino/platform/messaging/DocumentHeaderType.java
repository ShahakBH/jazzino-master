package com.yazino.platform.messaging;

import static org.apache.commons.lang3.Validate.notNull;

public enum DocumentHeaderType {
    TABLE("table"),
    PARTNER("partner"),
    IS_A_PLAYER("isAPlayer");

    private final String header;

    private DocumentHeaderType(final String header) {
        notNull(header, "header may not be null");

        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
