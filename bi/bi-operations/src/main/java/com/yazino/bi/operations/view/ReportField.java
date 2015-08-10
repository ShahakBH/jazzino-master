package com.yazino.bi.operations.view;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Presentation of a report field
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ReportField {
    /**
     * Ordinal position in the list of fields
     */
    int position();

    /**
     * Header text
     */
    String header() default "";

    /**
     * If we have a variable number of columns, this is the name of the field
     * that contains a list of headers for these multiple columns and determines their number
     */
    String multipleColumnsSource() default "";

    /**
     * Format of the column
     */
    ReportColumnFormat format() default STRING_DEFAULT;

    /**
     * Column width to set for the field. No change if left at -1
     */
    int columnWidth() default -1;

    /**
     * List of custom formats for different classes,
     * as returned by the field annotated with FormatClassSource
     */
    CustomColumnFormat[] customFormats() default {
    // Empty array
    };
}
