package com.yazino.bi.operations.controller;

import com.yazino.platform.Platform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ControllerModelHelper {
    private ControllerModelHelper() {
    }

    public static Map<String, String> listPlatforms(final Set<Platform> platformList) {
        final Map<String, String> platforms = new LinkedHashMap<String, String>();
        if (platformList.size() == Platform.values().length) {
            platforms.put("", "All platforms");
        }
        for (final Platform platform : platformList) {
            platforms.put(platform.name(), platform.name());
        }
        return platforms;
    }
}
