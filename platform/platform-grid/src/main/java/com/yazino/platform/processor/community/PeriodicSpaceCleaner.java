package com.yazino.platform.processor.community;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.service.community.GigaspaceCleanerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

public class PeriodicSpaceCleaner implements InitializingBean, DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicSpaceCleaner.class);

    private static final int ONE_HOUR = 60;
    private static final String PROPERTY_CLEANUP_PERIOD_MINS = "grid.cleanup.period-minutes";

    private final GigaspaceCleanerService gigaspaceCleanerService;
    private final YazinoConfiguration yazinoConfiguration;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    @Autowired
    public PeriodicSpaceCleaner(final GigaspaceCleanerService gigaspaceCleanerService,
                                final YazinoConfiguration yazinoConfiguration) {
        notNull(gigaspaceCleanerService, "gigaspaceCleanerService may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.gigaspaceCleanerService = gigaspaceCleanerService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public void destroy() throws Exception {
        LOG.info("stopping periodic event checker");
        try {
            // Shutting down the thread pool upon bean disposal
            scheduledFuture.cancel(true);
            scheduledFuture = null;
            executorService.shutdown();
        } catch (Exception e) {
            LOG.error("Could not shutdown the event checker", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("Initialising periodic space cleaner with current delay of {} minutes", delay());

        try {
            executorService = Executors.newScheduledThreadPool(1);
            scheduledFuture = executorService.scheduleAtFixedRate(new EventTask(), delay(), delay(), TimeUnit.MINUTES);

        } catch (Exception e) {
            LOG.error("Could not start periodic event checker", e);
        }
    }

    private int delay() {
        return yazinoConfiguration.getInt(PROPERTY_CLEANUP_PERIOD_MINS, ONE_HOUR);
    }

    public class EventTask implements Runnable {
        public void run() {
            try {
                gigaspaceCleanerService.removeOfflinePlayers();
            } catch (Exception e) {
                LOG.error("Removal of offline players failed", e);
            }
        }
    }
}
