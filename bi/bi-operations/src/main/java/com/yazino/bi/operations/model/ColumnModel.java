package com.yazino.bi.operations.model;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class ColumnModel {

    private static final Format DEFAULT_FORMATTER = new DefaultFormatter();

    private String name;
    private Format formatter;

    public ColumnModel(final String name) {
        this(name, DEFAULT_FORMATTER);
    }

    public ColumnModel(final String name, final Format formatter) {
        this.name = name;
        this.formatter = formatter;
    }

    public String getName() {
        return name;
    }

    public Format getFormatter() {
        return formatter;
    }

    public static class DefaultFormatter extends Format {

        private static final long serialVersionUID = -8842765193266565898L;

        @Override
        public StringBuffer format(final Object value,
                                   final StringBuffer buffer,
                                   final FieldPosition pos) {
            if (value != null) {
                buffer.append(value.toString());
            }
            return buffer;
        }

        @Override
        public Object parseObject(final String source,
                                  final ParsePosition pos) {
            throw new UnsupportedOperationException();
        }

    }
}
