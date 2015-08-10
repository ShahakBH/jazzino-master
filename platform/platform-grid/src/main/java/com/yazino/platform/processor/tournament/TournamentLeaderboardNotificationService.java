package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentLeaderboardUpdateRequest;
import com.yazino.platform.repository.tournament.TournamentRepository;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Reponsible for updating the tournament leaderboards and posting notification of player position changes.
 */
public class TournamentLeaderboardNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentLeaderboardNotificationService.class);

    private static final int THREAD_POOL_SIZE = 1;
    private static final int FIVE_SECONDS = 5 * 1000;
    private static final int SIXTY_SECONDS = 60 * 1000;

    private final TournamentRepository tournamentRepository;
    private final GigaSpace localGigaSpace;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;

    private long checkDelay = FIVE_SECONDS;

    @Autowired(required = true)
    public TournamentLeaderboardNotificationService(@Qualifier("tournamentRepository")
                                                    final TournamentRepository tournamentRepository,
                                                    @Qualifier("gigaSpace") final GigaSpace localGigaSpace) {
        notNull(tournamentRepository, "tournamentRepository may not be null");
        notNull(localGigaSpace, "localGigaSpace may not be null");

        this.tournamentRepository = tournamentRepository;
        this.localGigaSpace = localGigaSpace;
    }

    public void init() {
        LOG.info("Initialising with a delay of {}", checkDelay);

        executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

        final NotificationTask notificationTask = new NotificationTask();
        scheduledFuture = executorService.scheduleAtFixedRate(notificationTask, SIXTY_SECONDS, checkDelay, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduledFuture == null) {
            throw new IllegalStateException("Service has not been started");
        }

        LOG.info("Stopping service");

        scheduledFuture.cancel(true);
        scheduledFuture = null;

        executorService.shutdown();
    }

    public void setCheckDelay(final long checkDelay) {
        this.checkDelay = checkDelay;
    }

    public void process() {
        LOG.debug("Updating tournament leaderboards");

        for (final Tournament tournament : tournamentRepository.findLocalForLeaderboardUpdates()) {
            localGigaSpace.write(new TournamentLeaderboardUpdateRequest(tournament.getTournamentId()));
        }
    }

    public class NotificationTask implements Runnable {
        public void run() {
            try {
                process();

            } catch (Throwable e) {
                LOG.error("Tournament Leaderboard processing failed", e);
            }
        }
    }
}
