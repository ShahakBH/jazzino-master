package com.yazino.model.log;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogStorage {

    private static final int DEFAULT_CAPACITY = 500;
    public static final String ENTRY_FORMAT = "{\"c\":\"%s\",\"v\":\"%s\"}";

    private List<Message> messages = new LinkedList<Message>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int capacity;

    public LogStorage() {
        this(DEFAULT_CAPACITY);
    }

    public LogStorage(final int capacity) {
        this.capacity = capacity;
    }

    public void log(final String message) {
        log("", message);
    }

    public void log(final String category, final String message) {
        lock.writeLock().lock();
        try {
            if (messages.size() == capacity) {
                messages.remove(0);
            }
            messages.add(new Message(category, message));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String popJSON() {
        final StringBuilder builder = new StringBuilder("[");
        lock.writeLock().lock();
        try {
            for (int i = 0; i < messages.size(); i++) {
                final Message message = messages.get(i);
                builder.append(String.format(ENTRY_FORMAT, message.getCategory(),
                        StringEscapeUtils.escapeEcmaScript(message.getMessage())));
                if (i != messages.size() - 1) {
                    builder.append(",");
                }
            }
            builder.append("]");
            messages.clear();
        } finally {
            lock.writeLock().unlock();
        }
        return builder.toString();
    }


    private final class Message {
        private final String category;
        private final String message;

        private Message(final String category, final String message) {
            this.category = category;
            this.message = message;
        }

        public String getCategory() {
            return category;
        }

        public String getMessage() {
            return message;
        }
    }
}
