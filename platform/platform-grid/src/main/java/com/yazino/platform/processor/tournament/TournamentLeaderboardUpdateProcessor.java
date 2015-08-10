package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentLeaderboardUpdateRequest;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Responsible for processing enqueued leaderboard update requests.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TournamentLeaderboardUpdateProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentLeaderboardUpdateProcessor.class);

    private static final TournamentLeaderboardUpdateRequest TEMPLATE = new TournamentLeaderboardUpdateRequest();

    @Autowired(required = true)
    private TournamentHost tournamentHost;

    public void setTournamentHost(final TournamentHost tournamentHost) {
        this.tournamentHost = tournamentHost;
    }


    @EventTemplate
    public TournamentLeaderboardUpdateRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TournamentLeaderboardUpdateRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Process: %s", request.toString()));
        }
        try {
            notNull(request, "Request may not be null");
            notNull(request.getTournamentId(), "Tournament ID may not be null");
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
            return;
        }
        final BigDecimal tournamentId = request.getTournamentId();
        Tournament tournament = tournamentHost.getTournamentRepository().findById(tournamentId);
        if (tournament == null) {
            LOG.error("Tournament does not exist: " + tournamentId);
            return;
        }
        tournament = tournamentHost.getTournamentRepository().lock(tournamentId);
        try {
            tournament.updateLeaderboard(tournamentHost);
            tournamentHost.getTournamentRepository().save(tournament, false);
        } catch (Exception e) {
            LOG.error("Tournament leaderboard update failed for tournament " + tournamentId, e);
        }
    }
}
