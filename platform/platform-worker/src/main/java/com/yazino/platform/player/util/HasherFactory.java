package com.yazino.platform.player.util;

import com.yazino.platform.player.PasswordType;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class HasherFactory {

    private final Set<Hasher> hashers = new HashSet<Hasher>();
    private final Hasher preferred;

    public HasherFactory(final Set<Hasher> hashers,
                         final Hasher preferred) {
        notNull(hashers, "hashers may not be null");
        notNull(preferred, "preferred may not be null");

        this.hashers.addAll(hashers);
        this.preferred = preferred;
    }

    public Hasher getPreferred() {
        return preferred;
    }

    public Hasher forType(final PasswordType passwordType) {
        notNull(passwordType, "passwordType may not be null");

        for (Hasher hasher : hashers) {
            if (passwordType == hasher.getType()) {
                return hasher;
            }
        }

        throw new IllegalArgumentException("No hasher available for type " + passwordType);
    }

}
