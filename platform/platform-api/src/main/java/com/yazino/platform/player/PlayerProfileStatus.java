package com.yazino.platform.player;

import static org.apache.commons.lang3.Validate.notNull;

public enum PlayerProfileStatus {
    ACTIVE("A", true),
    BLOCKED("B", false),
    CLOSED("C", false);

    private final String id;
    private final boolean loginAllowed;

    private PlayerProfileStatus(final String id,
                                final boolean loginAllowed) {
        notNull(id, "id may not be null");

        this.id = id;
        this.loginAllowed = loginAllowed;
    }

    public String getId() {
        return id;
    }

    public boolean isLoginAllowed() {
        return loginAllowed;
    }

    public static PlayerProfileStatus forId(final String id) {
        for (PlayerProfileStatus playerProfileStatus : values()) {
            if (playerProfileStatus.getId().equals(id)) {
                return playerProfileStatus;
            }
        }
        throw new IllegalArgumentException("No status exists for ID " + id);
    }
}
