package com.yazino.bi.operations.view;

/**
 * Definition of the custom format for a given "class"
 */
public @interface CustomColumnFormat {
    /**
     * Name of the presentation class for which the format should be different
     */
    String value();

    /**
     * Format to apply for the custom class
     */
    ReportColumnFormat format();
}
