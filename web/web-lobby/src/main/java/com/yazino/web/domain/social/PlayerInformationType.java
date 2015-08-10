package com.yazino.web.domain.social;

public enum PlayerInformationType {
    NAME,
    PICTURE,
    BALANCE,
    LEVEL,
    ONLINE,
    LOCATIONS,
    PROVIDER,
    EXTERNAL_ID,
    NUMBER_OF_FRIENDS;

    public String getDisplayName() {
        return name().toLowerCase();
    }
}
