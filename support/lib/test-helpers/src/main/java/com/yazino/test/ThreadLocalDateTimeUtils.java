package com.yazino.test;

import org.joda.time.DateTimeUtils;

/**
 * A threaded-test safe class to lock JodaTime dates.
 * <p/>
 * This is a thread-local implementation of the MillisProvider that {@link DateTimeUtils} uses to
 * lock dates in place. This allows date modifications to remain local and hence multiple threads to lock their
 * own dates.
 */
public class ThreadLocalDateTimeUtils implements DateTimeUtils.MillisProvider {

    private static final ThreadLocal<Long> CURRENT_MILLIS = new ThreadLocal<Long>();
    private static final ThreadLocalDateTimeUtils INSTANCE = new ThreadLocalDateTimeUtils();

    public static void setCurrentMillisFixed(final long currentMillis) {
        DateTimeUtils.setCurrentMillisProvider(INSTANCE);
        CURRENT_MILLIS.set(currentMillis);
    }

    public static void setCurrentMillisSystem() {
        CURRENT_MILLIS.set(null);
    }

    @Override
    public long getMillis() {
        final Long currentMillis = CURRENT_MILLIS.get();
        if (currentMillis == null) {
            return System.currentTimeMillis();
        }
        return currentMillis;
    }
}
