package com.yazino.bi.operations.util;

import java.lang.reflect.Field;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.ReportColumnFormat;

/**
 * Information recovered from the report field
 */
public class ReportFieldInformation {
    private final Field field;

    private final Object fieldValue;

    private final String fieldHeader;

    private final int relativePosition;

    private final ReportColumnFormat format;

    private final CustomColumnFormat[] customFormats;

    private final int columnWidth;

    private String fieldValueFormatted;

    /**
     * Creates the field format information holder
     *
     * @param field            Reference of the field imported
     * @param fieldValue       Value for the individual field
     * @param fieldHeader      String header for the field
     * @param columnWidth      Width of the column, to be used in Excel reports
     * @param format           Formatting information for the individual cell
     * @param customFormats    Custom formatting parameters for the report
     * @param relativePosition If -1, just refers to the field value.
     *                         Otherwise, consider the field as a list and gets the value from a list's position
     */
    public ReportFieldInformation(final Field field, final Object fieldValue, final String fieldHeader,
            final ReportColumnFormat format, final int columnWidth, final CustomColumnFormat[] customFormats,
            final int relativePosition) {
        super();
        this.field = field;
        this.fieldValue = fieldValue;
        this.fieldHeader = fieldHeader;
        this.relativePosition = relativePosition;
        this.columnWidth = columnWidth;
        this.format = format;
        this.customFormats = customFormats;
    }

    public CustomColumnFormat[] getCustomFormats() {
        return customFormats;
    }

    public ReportColumnFormat getFormat() {
        return format;
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public int getRelativePosition() {
        return relativePosition;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    public String getFieldHeader() {
        return fieldHeader;
    }

    public Field getField() {
        return field;
    }

    public String getFieldValueFormatted() {
        return fieldValueFormatted;
    }

    public void setFieldValueFormatted(final String fieldValueFormatted) {
        this.fieldValueFormatted = fieldValueFormatted;
    }
}
