package com.yazino.platform;

public enum Platform {
    WEB,
    IOS,
    ANDROID,
    AMAZON,
    FACEBOOK_CANVAS;

    /**
     * Returns the Platform with the specified name. Case insensitive match is performed.
     * @return the Platform with given name. Null is returned if name is null, or doesn't match a Platform name.
     */
    public static Platform safeValueOf(String name) {
        Platform platform = null;
        try {
            platform = Platform.valueOf(name.toUpperCase());
        } catch (Exception e) {
            // do nothing
        }
        return platform;
    }
}


