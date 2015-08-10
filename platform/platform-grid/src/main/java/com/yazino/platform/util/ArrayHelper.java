package com.yazino.platform.util;

import com.google.common.base.Function;

public final class ArrayHelper {
    private ArrayHelper() {
        // utility class
    }

    public static <T, Q> Q[] convert(final T[] items,
                                     final Q[] toArray,
                                     final Function<T, Q> converter) {
        int index = 0;
        for (T item : items) {
            toArray[index++] = converter.apply(item);
        }
        return toArray;
    }
}
