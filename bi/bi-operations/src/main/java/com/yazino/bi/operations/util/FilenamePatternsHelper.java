package com.yazino.bi.operations.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helps to treat custom patterns in the file names
 */
public final class FilenamePatternsHelper {
    /**
     * No default constructor
     */
    private FilenamePatternsHelper() {
    }

    /**
     * If the file name contains the reference to the current date, replace it
     *
     * @param name File name
     * @return File name, eventually converted
     */
    public static String treatFileName(final String name) {
        if (!name.contains("#")) {
            return name;
        }
        final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return name.replace("#", format.format(new Date()));
    }
}
