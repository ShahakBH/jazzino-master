package com.yazino.platform.processor;

import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

public class PersistencePeriodicChecker<T extends PersistenceRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(PersistencePeriodicChecker.class);

    private static final int THREAD_POOL_SIZE = 1;
    private static final int ONE_MINUTE = 60 * 1000;

    private final Persister<T> persister;
    private final GigaSpace gigaSpace;
    private final ThreadPoolFactory threadPoolFactory;
    private final PersistenceRequest<T> template;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;
    private long pollingDelay = ONE_MINUTE;
    private long initialDelay = ONE_MINUTE;

    // cglib
    public PersistencePeriodicChecker() {
        this.gigaSpace = null;
        this.threadPoolFactory = null;
        this.persister = null;
        this.template = null;
    }

    @Autowired
    public PersistencePeriodicChecker(
            @Qualifier("gigaSpace") final GigaSpace gigaSpace,
            final ThreadPoolFactory threadPoolFactory,
            final Persister<T> persister,
            final PersistenceRequest<T> template) {
        notNull(gigaSpace, "gigaSpace null");
        notNull(threadPoolFactory, "threadPoolFactory is null");
        notNull(persister, "balancePersister is null");
        notNull(template, "template may not be null");

        this.gigaSpace = gigaSpace;
        this.threadPoolFactory = threadPoolFactory;
        this.persister = persister;
        this.template = template;
    }

    public void setPollingDelay(final long pollingDelay) {
        this.pollingDelay = pollingDelay;
    }

    public long getPollingDelay() {
        return pollingDelay;
    }

    public void setInitialDelay(final long initialDelay) {
        this.initialDelay = initialDelay;
    }

    @PostConstruct
    public void initialise() throws Exception {
        if (future != null) {
            throw new IllegalStateException("Expirer is already initialised");
        }

        LOG.info("Initialising: items with template {} will be persisted every {} millis", template, pollingDelay);

        executorService = threadPoolFactory.getScheduledThreadPool(THREAD_POOL_SIZE);

        final EventTask eventTask = new EventTask();
        future = executorService.scheduleAtFixedRate(
                eventTask, initialDelay, pollingDelay, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() throws Exception {
        LOG.info("Shutting down");

        try {
            future.cancel(true);
            future = null;

            runTaskDirectly(); // saving one last time

            executorService.shutdown();

        } catch (Exception e) {
            LOG.error("Could not shutdown the event checker", e);
        }
    }

    private void runTaskDirectly() {
        try {
            new EventTask().run();
        } catch (Exception e) {
            // ignored
        }
    }

    class EventTask implements Runnable {
        @Override
        public void run() {
            try {
                final PersistenceRequest<T>[] requests = retrieveRequests();
                if (requests != null) {
                    LOG.debug("Persisting {} requests.", requests.length);
                    for (PersistenceRequest<T> request : requests) {
                        processRequest(request);
                    }
                } else {
                    LOG.debug("Null requests returned for template {}", template);
                }

            } catch (Throwable e) {
                LOG.error("Error processing pending " + template.getClass().getName(), e);
            }
        }

        private void processRequest(final PersistenceRequest<T> request) {
            try {
                final PersistenceRequest<T> result = persister.persist(request);
                if (result != null) {
                    gigaSpace.write(result);
                }
            } catch (Exception e) {
                LOG.error("Error processing AccountBalancePersistenceRequest " + request, e);
            }
        }

        private PersistenceRequest<T>[] retrieveRequests() {
            try {
                return gigaSpace.takeMultiple(template, Integer.MAX_VALUE);

            } catch (Exception e) {
                LOG.error("Error retrieving {}", template.getClass().getName(), e);
            }
            return null;
        }
    }
}
