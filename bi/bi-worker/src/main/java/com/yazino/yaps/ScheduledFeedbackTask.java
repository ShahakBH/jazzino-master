package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.Set;

import static org.springframework.util.Assert.notNull;

/**
 * This class will periodically query apples feedback service for feedback regarding
 * devices that we should no longer be spamming.
 */
public class ScheduledFeedbackTask {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledFeedbackTask.class);

    private static final long SCHEDULE = 1000 * 60 * 60 * 24; // every day

    private final Map<String, FeedbackService> mServices;

    public ScheduledFeedbackTask(final Map<String, FeedbackService> services) {
        notNull(services);
        mServices = services;
    }

    @Scheduled(fixedDelay = SCHEDULE)
    public void go() {
        final Set<String> bundles = mServices.keySet();
        LOG.debug("Supported bundles are {}", bundles);

        for (String bundle : bundles) {
            try {
                FeedbackService service = mServices.get(bundle);
                service.readFeedback();
            } catch (Exception e) {
                LOG.error("Failed to read feedback for bundle [{}]", bundle, e);
            }
        }
    }

}
