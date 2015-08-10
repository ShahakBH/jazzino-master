package com.yazino.platform.processor.tournament;

import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.util.concurrent.ThreadPoolFactory;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.time.TimeSource;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Checks tournaments with pending events and enqueues request to process them.
 */
public class PeriodicTournamentChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicTournamentChecker.class);
    private static final int THREAD_POOL_SIZE = 1;

    private static final String QUERY_STRING = "nextEvent <= ?";
    private static final int TWO_MINUTES = 120000;
    private static final int ONE_HUNDRED_MS = 100;

    private final GigaSpace gigaSpace;
    private final ThreadPoolFactory threadPoolFactory;

    private long initialCheckDelay = TWO_MINUTES;
    private long checkDelay = ONE_HUNDRED_MS;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private TimeSource timeSource;

    @Autowired
    public PeriodicTournamentChecker(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     @Qualifier("threadPoolFactory") final ThreadPoolFactory threadPoolFactory,
                                     @Qualifier("timeSource") final TimeSource timeSource) {
        notNull(gigaSpace, "GigaSpace may not be null");
        notNull(threadPoolFactory, "Thread Pool Factory may not be null");
        notNull(timeSource, "Time Source may not be null");

        this.gigaSpace = gigaSpace;
        this.threadPoolFactory = threadPoolFactory;
        this.timeSource = timeSource;
    }

    public long getCheckDelay() {
        return checkDelay;
    }

    public void setCheckDelay(final long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public void setInitialCheckDelay(final long initialCheckDelay) {
        this.initialCheckDelay = initialCheckDelay;
    }

    public void init() {
        LOG.info("Initialising with delay of " + checkDelay);

        executorService = threadPoolFactory.getScheduledThreadPool(THREAD_POOL_SIZE);

        final EventTask eventTask = new EventTask();
        scheduledFuture = executorService.scheduleAtFixedRate(
                eventTask, initialCheckDelay, checkDelay, TimeUnit.MILLISECONDS);
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
            final SQLQuery<Tournament> query = new SQLQuery<Tournament>(Tournament.class, QUERY_STRING);
            try {
                query.setParameters(timeSource.getCurrentTimeStamp());

                final Tournament[] tournaments = gigaSpace.readMultiple(query, Integer.MAX_VALUE);
                for (final Tournament tournament : tournaments) {
                    final TournamentEventRequest processEventRequest
                            = new TournamentEventRequest(tournament.getTournamentId());
                    gigaSpace.write(processEventRequest);
                }

            } catch (Exception e) {
                LOG.error("Periodic event checking failed", e);
            }
        }
    }
}
