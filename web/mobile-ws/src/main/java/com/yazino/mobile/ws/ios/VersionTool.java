package com.yazino.mobile.ws.ios;

import org.apache.velocity.tools.config.DefaultKey;

import java.math.BigDecimal;

import static org.apache.commons.lang.Validate.notEmpty;

/**
 * Provides tools for dealing with versions.
 */
@DefaultKey("version")
public class VersionTool {

    public VersionTool() {
    }

    /**
     * Will make the specified version into a valid number.
     * i.e. 2.34.5 -> 2.34
     * 2-alpha -> 2
     *
     * @param version the version, never null
     * @return a string, never null
     */
    public static String toParseableNumber(String version) {
        notEmpty(version);
        try {
            return new BigDecimal(version).toPlainString();
        } catch (Exception e) {
            // ignore, expected for invalid numeric versions
        }
        StringBuilder builder = new StringBuilder(version);
        int suffixIndex = builder.indexOf("-");
        if (suffixIndex > 0) {
            builder.replace(suffixIndex, builder.length(), "");
        }
        if (builder.length() == 0) {
            throw new IllegalArgumentException("invalid version detected");
        }
        String[] components = builder.toString().split("\\.");
        if (components.length == 1) {
            return components[0];
        } else {
            return components[0] + "." + components[1];
        }
    }
}
