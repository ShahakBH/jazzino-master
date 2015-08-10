package com.yazino.platform.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

/**
 * SFL4J Logging implementation of an ErrorHandler.
 */
public class SLF4JLoggingErrorHandler implements ErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SLF4JLoggingErrorHandler.class);

    @Override
    public void handleError(final Throwable t) {
        LOG.warn("An error occurred: if transactions are enabled, rollback and re-submission will occur", t);
    }

}
