package com.yazino.platform.worker.message;

import static org.apache.commons.lang3.Validate.notNull;

public enum VerificationType {
    PLAYED("p"),
    NOT_PLAYED("n");

    private final String id;

    private VerificationType(final String id) {
        notNull(id, "id may not be null");

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static VerificationType forId(final String id) {
        if (id == null) {
            return null;
        }

        for (VerificationType verificationType : values()) {
            if (verificationType.getId().equals(id)) {
                return verificationType;
            }
        }

        return null;
    }
}
