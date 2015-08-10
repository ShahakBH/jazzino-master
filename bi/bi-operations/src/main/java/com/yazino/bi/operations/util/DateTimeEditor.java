package com.yazino.bi.operations.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Custom PropertyEditorSupport to convert from String to joda DateTime
 */
public class DateTimeEditor extends PropertyEditorSupport {
    private final DateTimeFormatter formatter;
    private final boolean allowEmpty;

    /**
     * Create a new DateTimeEditor instance, using the given format for
     * parsing and rendering.
     * <p/>
     * The "allowEmpty" parameter states if an empty String should be allowed
     * for parsing, i.e. get interpreted as null value. Otherwise, an
     * IllegalArgumentException gets thrown.
     *
     * @param dateFormat DateFormat to use for parsing and rendering
     * @param allowEmpty if empty strings should be allowed
     */
    public DateTimeEditor(final String dateFormat, final boolean allowEmpty) {
        this.formatter = DateTimeFormat.forPattern(dateFormat);
        this.allowEmpty = allowEmpty;
    }

    @Override
    public String getAsText() {
        final DateTime value = (DateTime) getValue();
        if (value != null) {
            return value.toString(formatter);
        } else {
            return "";
        }
    }

    @Override
    public void setAsText(final String text) {
        if (allowEmpty && !StringUtils.hasText(text)) {
            setValue(null);
        } else {
            setValue(new DateTime(formatter.parseDateTime(text)));
        }
    }
}
