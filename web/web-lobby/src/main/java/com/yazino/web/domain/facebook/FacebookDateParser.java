package com.yazino.web.domain.facebook;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;


public class FacebookDateParser {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookDateParser.class);

    private static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    private static final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("MM/dd/yy");
    private static final int TWO_DIGIT_YEAR_LIMIT = 100;

    public DateTime parseDate(final String text) {
        if (text == null || "".equals(text.trim())) {
            return null;
        }
        try {
            final DateTime parsedDate = new DateTime(FULL_FORMAT.parse(text).getTime());
            if (parsedDate.getYear() < TWO_DIGIT_YEAR_LIMIT) {
                final DateTime parsedShortDate = new DateTime(SHORT_FORMAT.parse(text).getTime());
                if (parsedShortDate.getYear() < TWO_DIGIT_YEAR_LIMIT) {
                    LOG.warn("Couldn't parse Facebook date: {}", text);
                    return null;
                }
                return parsedShortDate;
            }
            return parsedDate;

        } catch (ParseException e) {
            LOG.warn("Couldn't parse facebook date field: {}", text);
            return null;
        }
    }
}
