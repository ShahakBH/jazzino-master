package com.yazino.bi.operations.util;

/**
 * String formatting helper for some controllers
 */
public final class StringFormatHelper {
    private static final StringFormatHelper INSTANCE = new StringFormatHelper();

    /**
     * No public constructor
     */
    private StringFormatHelper() {
    }

    public static StringFormatHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Escapes the string to make it usable as a JQuery field name
     *
     * @param str Source string
     * @return String escaped
     */
    public String escape(final String str) {
        return str.replaceAll("\\.", "\\\\\\\\.").replaceAll("@", "\\\\\\\\@");
    }
}
