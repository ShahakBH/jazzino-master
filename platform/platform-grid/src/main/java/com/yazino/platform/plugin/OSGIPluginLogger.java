package com.yazino.platform.plugin;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

public class OSGIPluginLogger extends Logger {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(OSGIPluginLogger.class);

    @Override
    protected void doLog(final Bundle bundle, final ServiceReference sr, final int level, final String msg, final Throwable throwable) {
        final String logMessage = buildMessage(bundle, sr, msg, throwable);

        switch (level) {
            case LOG_DEBUG:
                LOG.debug(logMessage, process(throwable));
                break;
            case LOG_INFO:
                LOG.info(logMessage, process(throwable));
                break;
            case LOG_WARNING:
                LOG.warn(logMessage, process(throwable));
                break;
            case LOG_ERROR:
            default:
                LOG.error(logMessage, process(throwable));
                break;
        }
    }

    private String buildMessage(final Bundle bundle, final ServiceReference sr, final String msg, final Throwable throwable) {
        String logMessage = "";
        if (sr != null) {
            logMessage = logMessage + "SvcRef " + sr + " ";
        } else if (bundle != null) {
            logMessage = logMessage + "Bundle " + bundle.toString() + " ";
        }
        logMessage = logMessage + msg;
        if (throwable != null) {
            logMessage = logMessage + " (" + throwable + ")";
        }
        return logMessage;
    }

    private Throwable process(final Throwable throwable) {
        Throwable processedException = throwable;
        if ((processedException instanceof BundleException)
                && (((BundleException) processedException).getNestedException() != null)) {
            processedException = ((BundleException) processedException).getNestedException();
        }
        return processedException;
    }
}
