package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.RecurringTournamentDefinition;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.tournament.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Interval;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.time.SystemTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Processes recurring tournaments definitions, creating tournaments
 * if they are registering within the next x time period.
 */
public class RecurringTournamentPoller {
    private static final Logger LOG = LoggerFactory.getLogger(RecurringTournamentPoller.class);
    private static final int TEN_MINUTES = 10 * DateTimeConstants.MILLIS_PER_MINUTE;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final TournamentService tournamentService;

    private TimeSource timeSource = new SystemTimeSource();
    private long pollInterval = TEN_MINUTES;
    private long lookAheadPeriod = DateTimeConstants.MILLIS_PER_HOUR;

    @Autowired
    RecurringTournamentPoller(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                              @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                              final TournamentService tournamentService) {
        notNull(localGigaSpace, "localGigaSpace must not be null");
        notNull(globalGigaSpace, "globalGigaSpace must not be null");
        notNull(tournamentService, "tournamentService must not be null");

        this.tournamentService = tournamentService;
        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
    }

    public void start() {
        LOG.info("Tournament poller started with interval of {}", pollInterval);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    poll();
                } catch (Exception e) {
                    LOG.error("Failed to poll for recurring tournaments", e);
                }
            }
        }, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        LOG.info("Tournament poller stopped");

        executorService.shutdown();
    }

    public void poll() {
        LOG.debug("Polling for new tournaments to create.");

        final RecurringTournamentDefinition[] definitions = localGigaSpace.readMultiple(new RecurringTournamentDefinition(), Integer.MAX_VALUE);
        LOG.debug("Found [{}] recurring tournament definitions.", definitions.length);

        final long now = timeSource.getCurrentTimeStamp();
        for (RecurringTournamentDefinition definition : definitions) {
            if (!definition.isEnabled()) {
                continue;
            }

            LOG.debug("Processing enabled recurring tournament definitions [{}].", definition);

            final Set<DateTime> signupTimes = definition.calculateSignupTimes(new Interval(now, now + lookAheadPeriod));
            for (DateTime signupTime : signupTimes) {
                final TournamentDefinition tournament = toTournamentDefinition(definition, signupTime);
                final boolean exists = tournamentExists(definition, signupTime);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Tournament [{}] does {} exist so will {} be created", tournament, qualifierFor(exists), qualifierFor(!exists));
                }
                if (!exists) {
                    try {
                        final BigDecimal id = tournamentService.createTournament(tournament);
                        LOG.debug("Tournament [{}] created with id [{}]", tournament, id);

                    } catch (TournamentException e) {
                        LOG.error("Failed to create tournament [{}]", tournament, e);
                    }
                }
            }
        }
    }

    private String qualifierFor(final boolean exists) {
        if (exists) {
            return "";
        } else {
            return "not";
        }
    }

    private boolean tournamentExists(final RecurringTournamentDefinition definition,
                                     final DateTime signupTime) {
        return globalGigaSpace.count(toTournament(definition, signupTime)) > 0;
    }

    private TournamentDefinition toTournamentDefinition(final RecurringTournamentDefinition definition,
                                                        final DateTime nextSignupStart) {
        final Long signupPeriod = definition.getSignupPeriod();
        final DateTime startTime = new DateTime(nextSignupStart).plus(signupPeriod);

        return new TournamentDefinition(null,
                definition.getTournamentName(),
                definition.getTournamentVariationTemplate(),
                new DateTime(nextSignupStart),
                startTime,
                startTime,
                TournamentStatus.ANNOUNCED,
                definition.getPartnerId(),
                definition.getTournamentDescription());
    }

    private Tournament toTournament(final RecurringTournamentDefinition definition,
                                    final DateTime nextSignupStart) {
        final Long signupPeriod = definition.getSignupPeriod();
        final TournamentVariationTemplate template = definition.getTournamentVariationTemplate();
        final DateTime startTime = new DateTime(nextSignupStart).plus(signupPeriod);
        final Tournament tournament = new Tournament();
        tournament.setStartTimeStamp(startTime);
        tournament.setSignupEndTimeStamp(startTime);
        tournament.setSignupStartTimeStamp(new DateTime(nextSignupStart));
        tournament.setTournamentVariationTemplate(template);
        tournament.setName(definition.getTournamentName());
        tournament.setDescription(definition.getTournamentDescription());
        tournament.setPartnerId(definition.getPartnerId());
        return tournament;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(final long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public long getLookAheadPeriod() {
        return lookAheadPeriod;
    }

    public void setLookAheadPeriod(final long lookAheadPeriod) {
        this.lookAheadPeriod = lookAheadPeriod;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;
    }
}
