package com.yazino.bi.aggregator;

import com.yazino.configuration.YazinoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;

import static org.apache.commons.lang3.Validate.notNull;

public class LockRetryer {
    private static final Logger LOG = LoggerFactory.getLogger(LockRetryer.class);

    private static final String PROPERTY_LOCK_RETRIES = "lock.retries";
    private static final String PROPERTY_RETRY_DELAY_MS = "lock.retry-delay-ms";
    private static final int DEFAULT_MAX_LOCK_ATTEMPTS = 30;
    private static final int DEFAULT_LOCK_RETRY_DELAY_MS = 1000;

    private final Locker locker;
    private final YazinoConfiguration yazinoConfiguration;

    public LockRetryer(final Locker locker,
                       final YazinoConfiguration yazinoConfiguration) {
        notNull(locker, "locker may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.locker = locker;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    public boolean acquireLock() {
        final int maxLockAttempts = maxLockAttempts();
        int lockAttempt = 0;
        CannotAcquireLockException lastException;
        do {
            ++lockAttempt;
            try {
                LOG.debug("Attempting to acquire lock for {} (attempt {}/{})",
                        locker.getClass().getName(), lockAttempt, maxLockAttempts());
                return locker.lock();

            } catch (CannotAcquireLockException e) {
                lastException = e;
                sleepFor(retryDelayInMs());
            }

        } while (lockAttempt < maxLockAttempts);

        throw new CannotAcquireLockException(String.format("Unable to acquire lock for %s  after %s tries",
                locker.getClass().getName(), maxLockAttempts), lastException);
    }

    private int maxLockAttempts() {
        return yazinoConfiguration.getInt(PROPERTY_LOCK_RETRIES, DEFAULT_MAX_LOCK_ATTEMPTS);
    }

    private int retryDelayInMs() {
        return yazinoConfiguration.getInt(PROPERTY_RETRY_DELAY_MS, DEFAULT_LOCK_RETRY_DELAY_MS);
    }

    private void sleepFor(final int sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
            // ignored
        }
    }
}
