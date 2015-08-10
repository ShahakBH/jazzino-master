package com.yazino.web.domain.facebook;


import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FacebookDateParserTest {

    private FacebookDateParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new FacebookDateParser();
    }

    @Test
    public void shouldIgnoreNullString() {
        assertNull(parser.parseDate(null));
    }

    @Test
    public void shouldIgnoreEmptyString() {
        assertNull(parser.parseDate(""));
    }

    @Test
    public void shouldIgnoreInvalidFormat() {
        assertNull(parser.parseDate("03-11-1981"));
    }

    @Test
    public void shouldParseShortFormatsForLastCentury() {
        assertEquals(new DateTime(1981, 3, 11, 0, 0, 0, 0), parser.parseDate("03/11/81"));
    }

    @Test
    public void shouldParseShortFormatsForThisCentury() {
        assertEquals(new DateTime(2003, 3, 11, 0, 0, 0, 0), parser.parseDate("03/11/03"));
    }

    @Test
    public void shouldReturnNullIfYearIsNot4Or2Digits() {
        assertNull(parser.parseDate("03/11/3"));
    }

    @Test
    public void shouldParseValidDate() {
        assertEquals(new DateTime(1981, 3, 11, 0, 0, 0, 0), parser.parseDate("03/11/1981"));
    }
}
