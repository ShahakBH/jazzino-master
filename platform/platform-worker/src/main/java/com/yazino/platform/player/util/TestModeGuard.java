package com.yazino.platform.player.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class TestModeGuard {

    private final boolean testModeEnabled;

    @Autowired
    public TestModeGuard(@Value("${test-mode.enabled}") boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    public void assertTestModeEnabled() {
        if (!testModeEnabled) {
            throw new UnsupportedOperationException(
                    "This method is not available in production. Have you set the test-mode.enabled property?");
        }
    }
}
