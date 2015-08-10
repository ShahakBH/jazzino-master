package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.tournament.TournamentStatus;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A processor for tournament events.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 2)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TournamentEventProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentEventProcessor.class);

    private static final TournamentEventRequest TEMPLATE = new TournamentEventRequest();

    private final TournamentHost tournamentHost;
    private final GigaSpace gigaSpace;

    /**
     * CGLib constructor.
     */
    public TournamentEventProcessor() {
        tournamentHost = null;
        gigaSpace = null;
    }

    @Autowired
    public TournamentEventProcessor(@Qualifier("tournamentHost") final TournamentHost tournamentHost,
                                    @Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(tournamentHost, "Tournament Host may not be null");
        notNull(gigaSpace, "GigaSpace may not be null");

        this.tournamentHost = tournamentHost;
        this.gigaSpace = gigaSpace;
    }

    private void checkForInitialisation() {
        if (tournamentHost == null
                || gigaSpace == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public TournamentEventRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TournamentEventRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request: " + request);
        }

        try {
            checkForInitialisation();

            notNull(request, "Request may not be null");
            notNull(request.getTournamentId(), "Request: Tournament ID may not be null");
        } catch (Exception e) {
            LOG.error("Internal error", e);
            return;
        }

        Tournament tournament = tournamentHost.getTournamentRepository().findById(request.getTournamentId());
        if (tournament == null) {
            LOG.error("Tournament does not exist: " + request.getTournamentId());
            return;
        }

        tournament = tournamentHost.getTournamentRepository().lock(request.getTournamentId());
        try {
            if (tournament.calculateShouldSendWarningOfImpendingStart(tournamentHost)) {
                tournament.warningWasSent(tournamentHost);
            }
            tournament.processEvent(tournamentHost);
        } catch (Exception e) {
            LOG.error(String.format("Tournament %s: Event could not be processed", request.getTournamentId()), e);

            tournament.setTournamentStatus(TournamentStatus.ERROR);

            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            tournament.setMonitoringMessage("Error: " + stringWriter.toString());
            tournament.setNextEvent(null);
        }

        tournamentHost.getTournamentRepository().save(tournament, true);

        tryRemovingMatchingRequests(request);
    }

    private void tryRemovingMatchingRequests(final TournamentEventRequest request) {
        try {
            final TournamentEventRequest otherRequestsTemplate = new TournamentEventRequest(request.getTournamentId());
            gigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
        } catch (Throwable t) {
            LOG.error("Exception removing matching requests", t);
        }
    }
}
