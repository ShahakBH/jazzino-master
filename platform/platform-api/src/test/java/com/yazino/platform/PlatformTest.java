package com.yazino.platform;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

public class PlatformTest {
    @Test
    public void shouldReturnNullWhenNameIsNull() {
        assertNull(Platform.safeValueOf(null));
    }

    @Test
    public void shouldReturnPlatformWhenNameIsLowercase() {
        for (Platform platform: Platform.values()) {
            String lowerCaseName = platform.name().toLowerCase();
            assertThat(platform, is(Platform.safeValueOf(lowerCaseName)));
        }
    }

    @Test
    public void shouldReturnPlatformForAllKnownNames() {
        for (Platform platform: Platform.values()) {
            assertThat(platform, is(Platform.safeValueOf(platform.name())));
        }
    }

    @Test
    public void shouldReturnNullForUnknownPlatform() {
        assertNull(Platform.safeValueOf("this platform does not exist"));
    }
}
