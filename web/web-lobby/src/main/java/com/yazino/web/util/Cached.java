package com.yazino.web.util;

import com.yazino.game.api.time.SystemTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cached<T> implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    public static final int DEFAULT_LEASE_TIME = 1000;
    private final TimeSource timeSource;
    private final long leaseTime;
    private final Retriever<T> retriever;
    private final Map<String, CachedItem<T>> items = new ConcurrentHashMap<String, CachedItem<T>>();

    public Cached(final TimeSource timeSource,
                  final long leaseTime,
                  final Retriever<T> retriever) {
        this.timeSource = timeSource;
        this.leaseTime = leaseTime;
        this.retriever = retriever;
    }

    public Cached(final Retriever<T> retriever) {
        this.retriever = retriever;
        timeSource = new SystemTimeSource();
        leaseTime = DEFAULT_LEASE_TIME;
    }

    public T getItem(final String descriptor) {
        final CachedItem<T> item = items.get(descriptor);
        if (item == null || item.isExpired()) {
            items.put(descriptor, new CachedItem<T>(retriever.retrieve(descriptor), timeSource.getCurrentTimeStamp()));
        }
        return items.get(descriptor).getCached();
    }

    public interface Retriever<T> {
        T retrieve(String descriptor);
    }

    final class CachedItem<T> {
        private final T cached;
        private final long creationTime;

        private CachedItem(final T cached,
                           final long creationTime) {
            this.cached = cached;
            this.creationTime = creationTime;
        }

        public T getCached() {
            return cached;
        }

        public boolean isExpired() {
            return timeSource.getCurrentTimeStamp() - creationTime > leaseTime;
        }
    }

}
