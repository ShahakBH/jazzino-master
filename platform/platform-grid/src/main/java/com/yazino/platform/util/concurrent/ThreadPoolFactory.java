package com.yazino.platform.util.concurrent;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract source for a thread pool.
 */
public interface ThreadPoolFactory {

    ScheduledExecutorService getScheduledThreadPool(int size);

}
