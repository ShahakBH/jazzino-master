package com.yazino.test.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListAppender extends AppenderBase<ILoggingEvent> {

    public static ListAppender addAppenderTo(final Class loggerName) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger logger = lc.getLogger(loggerName);
        final ListAppender logAppender = new ListAppender();
        logAppender.setContext(logger.getLoggerContext());
        logger.addAppender(logAppender);

        logAppender.start();

        return logAppender;
    }

    private final List<String> messages = new ArrayList<String>();

    @Override
    protected void append(final ILoggingEvent eventObject) {
        if (eventObject instanceof ILoggingEvent) {
            final ILoggingEvent loggingEvent = eventObject;
            if (loggingEvent.getThrowableProxy() != null) {
                messages.add(loggingEvent.getLevel() + ": " + loggingEvent.getFormattedMessage() + "; "
                        + loggingEvent.getThrowableProxy().getMessage() + "; <trace>");
            } else {
                messages.add(loggingEvent.getLevel() + ": " + loggingEvent.getFormattedMessage());
            }
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
}
