package com.yazino.spring.mvc;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;

public class DateTimeEditor extends PropertyEditorSupport {

    private final List<DateTimeFormatter> allowableFormats = new ArrayList<DateTimeFormatter>(asList(
            ISODateTimeFormat.dateTime(),
            ISODateTimeFormat.date(),
            ISODateTimeFormat.basicDateTime(),
            ISODateTimeFormat.basicDateTimeNoMillis(),
            ISODateTimeFormat.basicDate()));

    @Override
    public String getAsText() {
        final DateTime dateTime = (DateTime) getValue();
        if (dateTime != null) {
            return dateTime.toString();
        }
        return null;

    }

    @Override
    public void setAsText(final String text) throws IllegalArgumentException {
        if (StringUtils.isBlank(text)) {
            setValue(null);

        } else {
            DateTime parsedDateTime = null;
            for (Iterator<DateTimeFormatter> i = allowableFormats.iterator(); parsedDateTime == null && i.hasNext(); ) {
                try {
                    parsedDateTime = i.next().parseDateTime(text);
                } catch (IllegalArgumentException e) {
                    // expected on occasion, and ignored
                }
            }

            if (parsedDateTime == null) {
                throw new IllegalArgumentException("Cannot parse date string:" + text);
            }

            setValue(parsedDateTime);
        }
    }
}
