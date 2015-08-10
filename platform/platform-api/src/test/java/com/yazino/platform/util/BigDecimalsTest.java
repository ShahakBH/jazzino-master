package com.yazino.platform.util;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class BigDecimalsTest {

    @Test
    public void strippingABigDecimalReturnsABigDecimalWithAScaleOfAtMostZero() {
        assertThat(BigDecimals.strip(new BigDecimal("12000.00")), is(equalTo(new BigDecimal("12000"))));
    }

    @Test
    public void strippingABigDecimalDoesNotChangeValuesWithAScaleOfZero() {
        assertThat(BigDecimals.strip(new BigDecimal("12000")), is(equalTo(new BigDecimal("12000"))));
    }

    @Test
    public void strippingABigDecimalDoesNotRemoveSignificantValues() {
        assertThat(BigDecimals.strip(new BigDecimal("12000.10230")), is(equalTo(new BigDecimal("12000.1023"))));
    }

    @Test
    public void strippingZerosFromNulLValueReturnsNull() {
        assertThat(BigDecimals.strip(null), is(nullValue()));
    }

    @Test
    public void testingEqualityByComparisonReturnsTrueForBigDecimalsDifferingOnlyByScale() {
        assertThat(BigDecimals.equalByComparison(new BigDecimal("10.10"), new BigDecimal("10.1")), is(equalTo(true)));
    }

    @Test
    public void testingEqualityByComparisonReturnsFalseForBigDecimalsDifferingByValue() {
        assertThat(BigDecimals.equalByComparison(new BigDecimal("10.11"), new BigDecimal("10.1")), is(equalTo(false)));
    }

    @Test
    public void testingEqualityByComparisonReturnsFalseWhenOnlyTheFirstArgumentIsNull() {
        assertThat(BigDecimals.equalByComparison(null, new BigDecimal("10.1")), is(equalTo(false)));
    }

    @Test
    public void testingEqualityByComparisonReturnsFalseWhenOnlyTheSecondArgumentIsNull() {
        assertThat(BigDecimals.equalByComparison(new BigDecimal("10.1"), null), is(equalTo(false)));
    }

    @Test
    public void testingEqualityByComparisonReturnsTrueWhenBothArgumentsAreNull() {
        assertThat(BigDecimals.equalByComparison(null, null), is(equalTo(true)));
    }

}
