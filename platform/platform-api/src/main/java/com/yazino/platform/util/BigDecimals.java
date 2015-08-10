package com.yazino.platform.util;

import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BigDecimals {

    private BigDecimals() {

    }

    public static BigDecimal scale(final BigDecimal bigDecimal, final int scale) {
        if (bigDecimal == null) {
            return null;
        }
        return bigDecimal.setScale(scale, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal strip(final BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return null;
        }
        if (bigDecimal.scale() == 0) {
            return bigDecimal;
        }

        final BigDecimal stripped = bigDecimal.stripTrailingZeros();
        if (stripped.scale() < 0) {
            return stripped.setScale(0);
        }
        return stripped;
    }

    public static boolean equalByComparison(final BigDecimal bigDecimal1, final BigDecimal bigDecimal2) {
        return ObjectUtils.compare(bigDecimal1, bigDecimal2) == 0;
    }

}
