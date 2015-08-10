package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

/**
 * Provides a basic logging {@link ErrorHandler}.
 */
public class LoggingErrorHandler implements ErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingErrorHandler.class);

    @Override
    public void handleError(final Throwable t) {
        LOG.warn("Error occurred, if transactions are enabled, rollback and re-submission will occur", t);
    }
}
