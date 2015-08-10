package com.yazino.util;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests the {@link com.yazino.util.DateUtils} class.
 */
public class DateUtilsTest {
    private final static long DATE_2010_Jul_15__09_22_20_BST = 1279182140299L;

    private static DateUtils utils;
    private static Locale locale;

    @BeforeClass
    public static void init() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DATE_2010_Jul_15__09_22_20_BST);
        locale = Locale.getDefault();
        Locale.setDefault(Locale.UK);
        utils = new DateUtils();
    }

    @AfterClass
    public static void tearDown() {
        Locale.setDefault(locale);
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getShortFormMonthsOfYear_returns12ShortCorrectMonths() throws Exception {
        final String[] expected =
                {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        assertArrayEquals(expected, utils.getShortFormMonthsOfYear());
    }

    @Test
    public void getYearsUntil_endYearSameAsCurrentYear_returnsNowYear() throws Exception {
        final int[] expected = {2010};
        assertArrayEquals(expected, utils.getYearsUntil(2010));
    }

    @Test
    public void getYearsUntil_endYearLessThanCurrentYear_returnsYearsFromEndYearToNow() throws Exception {
        final int[] expected = {2010, 2009, 2008, 2007, 2006, 2005, 2004, 2003, 2002, 2001, 2000};
        assertArrayEquals(expected, utils.getYearsUntil(2000));
    }

    @Test
    public void getYearsUntil_endYearGreaterThanCurrentYear_returnsYearsFromNowToIncludingEndYear() throws Exception {
        final int[] expected = {2010, 2011, 2012, 2013, 2014, 2015};
        assertArrayEquals(expected, utils.getYearsUntil(2015));
    }
}
