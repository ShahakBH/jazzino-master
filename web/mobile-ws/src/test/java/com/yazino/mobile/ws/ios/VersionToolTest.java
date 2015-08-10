package com.yazino.mobile.ws.ios;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class VersionToolTest {

    @Test
    public void shouldReturnSameNumberIfSafe() throws Exception {
        assertEquals("23", VersionTool.toParseableNumber("23"));
        assertEquals("1.2", VersionTool.toParseableNumber("1.2"));
        assertEquals("8.25", VersionTool.toParseableNumber("8.25"));
        assertEquals("8.90", VersionTool.toParseableNumber("8.90"));
    }

    @Test
    public void shouldIgnoreBuild() throws Exception {
        assertEquals("23.1", VersionTool.toParseableNumber("23.1.2"));
        assertEquals("5.122", VersionTool.toParseableNumber("5.122.8"));
    }

    @Test
    public void shouldIgnoreSuffix() throws Exception {
        assertEquals("23.1", VersionTool.toParseableNumber("23.1.2-alpha1"));
        assertEquals("23", VersionTool.toParseableNumber("23-alpha1"));
        assertEquals("23.5", VersionTool.toParseableNumber("23.5-alpha1"));
    }
}
