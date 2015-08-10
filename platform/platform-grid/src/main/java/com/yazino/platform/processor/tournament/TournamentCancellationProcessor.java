package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.tournament.TournamentStatus;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
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
 * A processor for request tournament cancellation.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class TournamentCancellationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentCancellationProcessor.class);

    private static final Tournament TEMPLATE = new Tournament();
    private final TournamentHost tournamentHost;

    static {
        TEMPLATE.setTournamentStatus(TournamentStatus.CANCELLING);
    }

    /**
     * CGLib constructor.
     */
    public TournamentCancellationProcessor() {
        tournamentHost = null;
    }

    @Autowired
    public TournamentCancellationProcessor(@Qualifier("tournamentHost") final TournamentHost tournamentHost) {
        notNull(tournamentHost, "Tournament Host may not be null");

        this.tournamentHost = tournamentHost;
    }

    private void checkForInitialisation() {
        if (tournamentHost == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public Tournament eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public Tournament process(final Tournament tournament) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing tournament: " + tournament);
        }

        try {
            checkForInitialisation();

            notNull(tournament, "Request may not be null");
            notNull(tournament.getTournamentId(), "Request: Tournament ID may not be null");

        } catch (Exception e) {
            LOG.error("Internal error", e);
            return tournament;
        }

        try {
            tournament.cancel(tournamentHost);
        } catch (Exception e) {
            LOG.error(String.format("Tournament %s: Cancellation could not be processed",
                    tournament.getTournamentId()), e);

            tournament.setTournamentStatus(TournamentStatus.ERROR);

            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            tournament.setMonitoringMessage("Error: " + stringWriter.toString());
        }

        tournamentHost.getTournamentRepository().save(tournament, true);

        return tournament;
    }
}
