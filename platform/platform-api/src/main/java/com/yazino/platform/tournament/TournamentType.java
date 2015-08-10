package com.yazino.platform.tournament;

import static org.apache.commons.lang3.Validate.notNull;

public enum TournamentType {
    SITNGO("Sit'n Go"), PRESET("Preset");

    private final String displayName;

    /**
     * Create a new type with the given display name.
     *
     * @param displayName the name to represent this type in user display.
     */
    private TournamentType(final String displayName) {
        notNull(displayName, "Display Name may not be null");
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
