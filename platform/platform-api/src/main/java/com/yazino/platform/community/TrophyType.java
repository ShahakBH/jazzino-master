package com.yazino.platform.community;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum TrophyType {

    WEEKLY_CHAMP("trophy_weeklyChamp"),
    MEDAL("medal_1", "medal_2", "medal_3");

    private final Set<String> names;

    TrophyType(final String... names) {
        this.names = new HashSet<String>(Arrays.asList(names));
    }

    public Set<String> getNames() {
        return names;
    }
}
