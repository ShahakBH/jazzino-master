package com.yazino.platform.model.statistic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NotificationMessageTest {
    @Test
    public void shouldApplyParameters() {
        final NotificationMessage underTest = new NotificationMessage("Hello, %s", "World!");
        assertEquals("Hello, World!", underTest.getMessage());
    }

    @Test
    public void shouldIgnoreStringFormatErrors() {
        final NotificationMessage underTest = new NotificationMessage("Where are my %s?");
        assertEquals("Where are my %s?", underTest.getMessage());
    }
}
