package com.yazino.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.LogManager;

/**
 * Install the java.util.logging forwarder for SLF4J.
 * <p/>
 * This shouldn't generally be used with Log4J due to the performance
 * penalty.
 *
 * @see "http://www.slf4j.org/legacy.html#jul-to-slf4j"
 * @see "http://logback.qos.ch/manual/configuration.html#LevelChangePropagator"
 */
public class JULLoggingForwarder {
    private static final Logger LOG = LoggerFactory.getLogger(JULLoggingForwarder.class);

    public void initialise() {
        LOG.info("Installing SLF4J Bridge");

        removeDefaultJULLoggingHandlers();

        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.install();
        }
    }

    public void shutdown() {
        LOG.info("Uninstalling SLF4J Bridge");

        if (SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.uninstall();
        }
    }

    private void removeDefaultJULLoggingHandlers() {
        final Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            removeAllHandlersFrom(LogManager.getLogManager().getLogger(loggerNames.nextElement()));
        }
        final java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
        removeAllHandlersFrom(rootLogger);
    }

    private void removeAllHandlersFrom(final java.util.logging.Logger logger) {
        if (logger != null) {
            for (final Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
                try {
                    handler.close();
                } catch (Exception ex) {
                    // ignored
                }
            }
        }
    }

}
