package com.yazino.logging.appender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListAppender<E> extends AppenderBase<E> {

    private final List<String> messages = new ArrayList<String>();

    @Override
    protected void append(final E eventObject) {
        if (eventObject instanceof ILoggingEvent) {
            messages.add(((ILoggingEvent) eventObject).getFormattedMessage());
        } else {
            System.err.println("Cannot handle type: "
                    + eventObject.getClass().getName() + ": " + eventObject);
        }
    }

    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public synchronized void clear() {
        messages.clear();
    }

    public static ListAppender addTo(final Class loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);

        return addAppenderTo(logger);
    }

    public static ListAppender addTo(final String loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);

        return addAppenderTo(logger);
    }

    private static ListAppender addAppenderTo(final Logger logger) {
        final ListAppender<ILoggingEvent> logAppender = new ListAppender<ILoggingEvent>();
        logAppender.setContext(logger.getLoggerContext());
        logger.addAppender(logAppender);

        logAppender.start();

        return logAppender;
    }
}
