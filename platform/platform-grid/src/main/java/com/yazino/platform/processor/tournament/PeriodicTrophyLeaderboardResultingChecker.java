package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultingRequest;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.time.TimeSource;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Checks tournaments with pending events and enqueues request to process them.
 */
public class PeriodicTrophyLeaderboardResultingChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicTrophyLeaderboardResultingChecker.class);
    private static final int THREAD_POOL_SIZE = 1;
    private static final int ONE_HUNDRED_MS = 100;

    private final GigaSpace gigaSpace;
    private final TrophyLeaderboardRepository trophyLeaderboardRepository;
    private final ThreadPoolFactory threadPoolFactory;

    private long checkDelay = ONE_HUNDRED_MS;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private TimeSource timeSource;

    @Autowired
    public PeriodicTrophyLeaderboardResultingChecker(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                                     final ThreadPoolFactory threadPoolFactory,
                                                     @Qualifier("trophyLeaderboardRepository") final TrophyLeaderboardRepository trophyLeaderboardRepository,
                                                     final TimeSource timeSource) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(threadPoolFactory, "threadPoolFactory may not be null");
        notNull(trophyLeaderboardRepository, "trophyLeaderboardRepository may not be null");
        notNull(timeSource, "timeSource may not be null");

        this.gigaSpace = gigaSpace;
        this.threadPoolFactory = threadPoolFactory;
        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
        this.timeSource = timeSource;
    }

    public void setCheckDelay(final long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public void init() {
        LOG.info("Initialising with delay of {}", checkDelay);

        executorService = threadPoolFactory.getScheduledThreadPool(THREAD_POOL_SIZE);

        final EventTask eventTask = new EventTask();
        scheduledFuture = executorService.scheduleAtFixedRate(
                eventTask, checkDelay, checkDelay, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduledFuture == null) {
            throw new IllegalStateException("Scheduled check has not been started");
        }

        LOG.info("Stopping checker");

        scheduledFuture.cancel(true);
        scheduledFuture = null;

        executorService.shutdown();
    }

    class EventTask implements Runnable {
        public void run() {
            try {
                final Set<TrophyLeaderboard> resultingRequired
                        = trophyLeaderboardRepository.findLocalResultingRequired(timeSource);
                for (final TrophyLeaderboard trophyLeaderboard : resultingRequired) {
                    final TrophyLeaderboardResultingRequest resultingRequest
                            = new TrophyLeaderboardResultingRequest(trophyLeaderboard.getId());
                    gigaSpace.write(resultingRequest);
                }

            } catch (Exception e) {
                LOG.error("Periodic tournament resulting checking failed", e);
            }
        }
    }
}
