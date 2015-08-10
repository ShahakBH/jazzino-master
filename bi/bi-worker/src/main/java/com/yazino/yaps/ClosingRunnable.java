package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A simple runnable which closes any object as specified by {@link QuietCloser}.
 */
public class ClosingRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClosingRunnable.class);

    private final Object toClose;

    public ClosingRunnable(final Object toClose) {
        notNull(toClose, "socket was null");
        this.toClose = toClose;
    }

    @Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("About to close [%s]", toClose));
        }
        QuietCloser.closeQuietly(toClose);
    }

    final Object getToClose() {
        return toClose;
    }
}
