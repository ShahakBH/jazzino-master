package com.yazino.bi.operations.util;

import static org.junit.Assert.*;
import static com.yazino.bi.operations.util.DataFormatHelper.*;

import org.junit.Test;

public class DataFormatHelperTest {
    @Test
    public void shouldCorrectlyShowFormattedData() {
        // GIVEN the source data
        final int srcInt = 1024;
        final double srcDouble = 0.125D;

        // WHEN applying the helper
        final String percentageString = formatPercentage(srcDouble);
        final String doubleString = formatDouble(srcDouble);
        final String dollarString = formatDollars(srcDouble);
        final String euroString = formatEuros(srcDouble);
        final String poundString = formatPounds(srcDouble);
        final String intString = formatInteger(srcInt);

        // THEN the formatted strings are behaving as expected
        assertEquals("12,500%", percentageString);
        assertEquals("0,13", doubleString);
        assertEquals("$0,13", dollarString);
        assertEquals("0,13&euro;", euroString);
        assertEquals("&pound;0,13", poundString);
        assertEquals("1", intString.substring(0, 1));
        assertEquals("024", intString.substring(2));
    }

    @Test
    public void shouldGetUniqueInstance() {
        // GIVEN an instance of the helper
        final DataFormatHelper helper1 = getInstance();

        // WHEN getting another instance
        final DataFormatHelper helper2 = getInstance();

        // THEN it is not null and reference-equal to the first one
        assertTrue(helper1 == helper2);
    }
}
