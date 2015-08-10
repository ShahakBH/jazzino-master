package com.yazino.email;

import static org.apache.commons.lang3.Validate.notNull;

public enum EmailVerificationStatus {

    /**
     * Address is valid.
     */
    VALID("V"),

    /**
     * Address is from a provider that will accept all addresses and therefore cannot be confirmed valid.
     */
    ACCEPT_ALL("A"),

    /**
     * We cannot determine the state of the address.
     */
    UNKNOWN("U"),

    /**
     * The address could not be checked due to a temporary malfunction, for example the
     * remote service was unavailable or the network was down.
     */
    UNKNOWN_TEMPORARY("T"),

    /**
     * The address is invalid.
     */
    INVALID("I"),

    /**
     * The address is malformed.
     */
    MALFORMED("M");

    private final String id;

    private EmailVerificationStatus(final String id) {
        notNull(id, "id may not be null");

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static EmailVerificationStatus forId(final String id) {
        for (EmailVerificationStatus emailVerificationStatus : values()) {
            if (emailVerificationStatus.getId().equals(id)) {
                return emailVerificationStatus;
            }
        }
        throw new IllegalArgumentException("Invalid ID: " + id);
    }
}
