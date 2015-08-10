package com.yazino.bi.operations.util;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class StringFormatHelperTest {
    private StringFormatHelper underTest = StringFormatHelper.getInstance();

    @Test
    public void shouldEscapeStringsWithPoint() {
        // GIVEN the source string with point
        final String testString = "test.string";

        // WHEN escaping the string
        final String result = underTest.escape(testString);

        // THEN the result is the escaped string
        assertThat(result, is("test\\\\.string"));
    }
}
