package com.yazino.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Install the java.util.logging forwarder for SLF4J via a {@link ServletContextListener}.
 * <p/>
 * This shouldn't generally be used with Log4J due to the performance
 * penalty.
 *
 * @see "http://www.slf4j.org/legacy.html#jul-to-slf4j"
 * @see "http://logback.qos.ch/manual/configuration.html#LevelChangePropagator"
 */
public class JULLoggingForwarderContextListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(JULLoggingForwarderContextListener.class);

    private final JULLoggingForwarder forwarder = new JULLoggingForwarder();

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        forwarder.initialise();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        forwarder.shutdown();
    }

}

