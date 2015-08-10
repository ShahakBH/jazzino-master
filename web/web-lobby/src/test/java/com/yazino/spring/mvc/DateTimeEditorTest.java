package com.yazino.spring.mvc;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateTimeEditorTest {

    private DateTimeEditor underTest;

    @Before
    public void init() {
        underTest = new DateTimeEditor();
    }

    @Test
    public void shouldReturnCorrectString() {
        underTest.setValue(new DateTime(1337));
        assertEquals("1970-01-01T01:00:01.337+01:00", underTest.getAsText());
    }

    @Test
    public void shouldReturnCorrectISODateTime() {
        underTest.setAsText("1970-01-01T01:00:01.337+01:00");
        assertEquals(new DateTime(1337), underTest.getValue());
    }


    @Test
    public void shouldReturnCorrectBasicDateTimeWithMillis() {
        underTest.setAsText("19700101T010001.337+0100");
        assertEquals(new DateTime(1337), underTest.getValue());
    }

    @Test
    public void shouldReturnCorrectBasicDateTime() {
        underTest.setAsText("19700101T010000+0100");
        assertEquals(new DateTime(1970, 1, 1, 1, 0), underTest.getValue());
    }

    @Test
    public void shouldReturnCorrectISODate() {
        underTest.setAsText("1970-01-01");
        assertEquals(new DateTime(1970, 1, 1, 0, 0), underTest.getValue());
    }

    @Test
    public void shouldReturnCorrectBasicDate() {
        underTest.setAsText("19700101");
        assertEquals(new DateTime(1970, 1, 1, 0, 0), underTest.getValue());
    }

    @Test
    public void shouldReturnNullIfStringEmpty() {
        underTest.setAsText(" ");
        assertNull(underTest.getValue());
    }

    @Test
    public void shouldOnGetAsTextReturnNullIfValueNull() {
        underTest.setAsText(null);
        assertNull(underTest.getAsText());
    }
}
