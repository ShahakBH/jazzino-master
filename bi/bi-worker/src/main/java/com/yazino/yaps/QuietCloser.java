package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Will attempt to close / call close() on any object suppressing any checked exceptions.
 */
public final class QuietCloser {
    private static final Logger LOG = LoggerFactory.getLogger(QuietCloser.class);

    private QuietCloser() {
    }

    public static void closeQuietly(final Object toClose) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing " + toClose);
        }
        try {
            final Method close = toClose.getClass().getMethod("close");
            close.invoke(toClose);
        } catch (Exception e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Failed to close %s, exception was %s", toClose, e.getMessage()));
            }
        }
    }
}
