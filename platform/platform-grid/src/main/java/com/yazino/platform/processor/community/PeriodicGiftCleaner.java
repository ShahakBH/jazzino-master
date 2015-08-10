package com.yazino.platform.processor.community;

import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.service.community.GiftProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

public class PeriodicGiftCleaner {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicGiftCleaner.class);

    private static final int TEN_MINUTES = 600000;
    private static final int CLEAN_PERIOD = TEN_MINUTES;

    private final GiftRepository giftRepository;
    private final GiftProperties giftProperties;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    @Autowired
    public PeriodicGiftCleaner(final GiftRepository giftRepository,
                               final GiftProperties giftProperties) {
        this(giftRepository, giftProperties, Executors.newScheduledThreadPool(1));
    }

    public PeriodicGiftCleaner(final GiftRepository giftRepository,
                               final GiftProperties giftProperties,
                               final ScheduledExecutorService executorService) {
        notNull(giftRepository, "giftRepository may not be null");
        notNull(giftProperties, "giftingProperties may not be null");
        notNull(executorService, "executorService may not be null");

        this.giftRepository = giftRepository;
        this.giftProperties = giftProperties;
        this.executorService = executorService;
    }

    @PostConstruct
    public void initialise() {
        LOG.info("Initialising with delay of {}", CLEAN_PERIOD);

        try {
            scheduledFuture = executorService.scheduleAtFixedRate(new CleanUpTask(), CLEAN_PERIOD, CLEAN_PERIOD, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            LOG.error("Could not start scheduled service", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Stopping");
        try {
            if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
                scheduledFuture.cancel(true);
            }
            scheduledFuture = null;
            executorService.shutdown();

        } catch (Exception e) {
            LOG.error("Shutdown failed", e);
        }
    }

    public class CleanUpTask implements Runnable {
        public void run() {
            try {
                giftRepository.cleanUpOldGifts(giftProperties.retentionInHours());

            } catch (Exception e) {
                LOG.error("Clean up of old gifts failed", e);
            }
        }
    }
}
