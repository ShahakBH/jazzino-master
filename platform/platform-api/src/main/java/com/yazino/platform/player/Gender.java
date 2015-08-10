package com.yazino.platform.player;

import static org.apache.commons.lang3.Validate.notNull;

public enum Gender {
    MALE("M", "Male"),
    FEMALE("F", "Female"),
    OTHER("O", "Other");

    private final String id;
    private final String name;

    private Gender(final String id,
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
     * Find a gender by the ID.
     *
     * @param id the ID.
     * @return the gender, or null if not matched.
     */
    public static Gender getById(final String id) {
        if (id == null) {
            return null;
        }

        for (final Gender gender : values()) {
            if (gender.getId().equals(id)) {
                return gender;
            }
        }

        return null;
    }
}
