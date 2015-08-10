package com.yazino.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VelocityCommonsLogChute implements LogChute {
    private static final Logger LOG = LoggerFactory.getLogger(VelocityCommonsLogChute.class);

    @Override
    public void init(final RuntimeServices runtimeServices) throws Exception {
    }

    @Override
    public void log(final int logLevel, final String message) {
        switch (logLevel) {
            case LogChute.TRACE_ID:
                LOG.trace(message);
                return;
            case LogChute.DEBUG_ID:
                LOG.debug(message);
                return;
            case LogChute.INFO_ID:
                LOG.info(message);
                return;
            case LogChute.WARN_ID:
                LOG.warn(message);
                return;
            case LogChute.ERROR_ID:
                LOG.error(message);
                return;
            default:
                break;
        }
    }

    @Override
    public void log(final int logLevel, final String message, final Throwable throwable) {
        switch (logLevel) {
            case LogChute.TRACE_ID:
                LOG.trace(message, throwable);
                return;
            case LogChute.DEBUG_ID:
                LOG.debug(message, throwable);
                return;
            case LogChute.INFO_ID:
                LOG.info(message, throwable);
                return;
            case LogChute.WARN_ID:
                LOG.warn(message, throwable);
                return;
            case LogChute.ERROR_ID:
                LOG.error(message, throwable);
                return;
            default:
                break;
        }
    }

    @Override
    public boolean isLevelEnabled(final int logLevel) {
        switch (logLevel) {
            case LogChute.TRACE_ID:
                return LOG.isTraceEnabled();
            case LogChute.DEBUG_ID:
                return LOG.isDebugEnabled();
            case LogChute.INFO_ID:
                return LOG.isInfoEnabled();
            case LogChute.WARN_ID:
                return LOG.isWarnEnabled();
            case LogChute.ERROR_ID:
                return LOG.isErrorEnabled();
            default:
                return false;
        }
    }
}
