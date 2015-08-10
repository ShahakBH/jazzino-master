package com.yazino.platform.player;

import static org.apache.commons.lang3.Validate.notNull;

public enum GuestStatus {
    NON_GUEST("N", "Non-guest"),
    GUEST("G", "Guest"),
    CONVERTED("C", "Converted");

    private final String id;
    private final String name;

    private GuestStatus(final String id,
                        final String name) {
        notNull(id, "ID may not be null");
        notNull(name, "Name may not be null");

        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Find a registration status by the ID.
     *
     * @param id the ID.
     * @return the registration status, or null if not matched.
     */
    public static GuestStatus getById(final String id) {
        if (id == null) {
            return null;
        }

        for (final GuestStatus status : values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }

        return null;
    }
}
