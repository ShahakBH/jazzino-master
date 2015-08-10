package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentEventRequest;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.tournament.TournamentTableService;
import com.yazino.platform.tournament.TournamentStatus;
import org.openspaces.core.GigaSpace;
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
import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A processor for removing closed tournaments from the space.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class TournamentClosureProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentClosureProcessor.class);

    private static final Tournament TEMPLATE = new Tournament();

    private final GigaSpace gigaSpace;
    private final TournamentRepository tournamentRepository;
    private final TournamentTableService tournamentTableService;

    static {
        TEMPLATE.setTournamentStatus(TournamentStatus.CLOSED);
    }

    /**
     * CGLib constructor.
     */
    protected TournamentClosureProcessor() {
        gigaSpace = null;
        tournamentRepository = null;
        tournamentTableService = null;
    }

    @Autowired
    public TournamentClosureProcessor(final TournamentTableService tournamentTableService,
                                      @Qualifier("tournamentRepository") final TournamentRepository tournamentRepository,
                                      @Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(tournamentTableService, "Tournament Table Service may not be null");
        notNull(gigaSpace, "GigaSpace may not be null");
        notNull(tournamentRepository, "Tournament Repository may not be null");

        this.tournamentTableService = tournamentTableService;
        this.gigaSpace = gigaSpace;
        this.tournamentRepository = tournamentRepository;
    }

    private void checkForInitialisation() {
        if (gigaSpace == null
                || tournamentTableService == null
                || tournamentRepository == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public Tournament eventTemplate() {
        return TEMPLATE;
    }

    /**
     * Removes the tournament from the space.
     * <p/>
     * We rely on the process take to remove the tournament, therefore you should ensure that the tournament is never
     * returned from the method, or an exclusive read used for this processor.
     *
     * @param tournament the tournament to be removed from the space.
     * @return the tournament on failure, or null on success.
     */
    @SpaceDataEvent
    public Tournament process(final Tournament tournament) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing request: " + tournament);
        }

        try {
            checkForInitialisation();

            notNull(tournament, "Request may not be null");
            notNull(tournament.getTournamentId(), "Request: Tournament ID may not be null");

        } catch (Exception e) {
            LOG.error("Internal error", e);

            tournament.setTournamentStatus(TournamentStatus.ERROR);
            tournament.setMonitoringMessage("Error: " + getExceptionText(e));
            return tournament;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing tournament " + tournament.getTournamentId());
        }

        try {
            cleanUpTournamentResources(tournament);

            final TournamentEventRequest eventTemplate = new TournamentEventRequest(tournament.getTournamentId());
            gigaSpace.takeMultiple(eventTemplate, Integer.MAX_VALUE);

            tournamentRepository.remove(tournament);

        } catch (Throwable t) {
            LOG.error("Tournament " + tournament.getTournamentId() + " closure failed", t);

            tournament.setTournamentStatus(TournamentStatus.ERROR);

            tournament.setMonitoringMessage("Error: " + getExceptionText(t));
            return tournament;
        }

        return null;
    }

    private void cleanUpTournamentResources(final Tournament tournament) {

        final List<BigDecimal> usedTables = tournament.getStartingTables();

        if (usedTables != null && usedTables.size() > 0) {
            tournamentTableService.removeTables(usedTables);
        }
    }

    private String getExceptionText(final Throwable e) {
        final StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
