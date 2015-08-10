package com.yazino.platform.util.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation using the JavaX Concurrent package.
 */
public class JavaThreadPoolFactory implements ThreadPoolFactory {

    @Override
    public ScheduledExecutorService getScheduledThreadPool(final int size) {
        return Executors.newScheduledThreadPool(size);
    }

}
