package com.yazino.platform.processor.tournament;

import com.google.common.base.Function;
import com.yazino.platform.model.tournament.AwardMedalsRequest;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A processor for tournament events.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
public class TournamentSettlementProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentSettlementProcessor.class);

    private static final Tournament TEMPLATE = new Tournament();


    static {
        TEMPLATE.setTournamentStatus(TournamentStatus.FINISHED);
    }

    private final TournamentHost tournamentHost;
    private final TournamentSummaryRepository tournamentSummaryRepository;
    private Function<Tournament, TournamentSummary> tournamentToSummaryTransformer;

    /**
     * CGLib constructor.
     */
    public TournamentSettlementProcessor() {
        tournamentHost = null;
        tournamentSummaryRepository = null;
    }

    @Autowired
    public TournamentSettlementProcessor(@Qualifier("tournamentHost") final TournamentHost tournamentHost,
                                         final TournamentSummaryRepository tournamentSummaryRepository,
                                         @Qualifier("tournamentToSummaryTransformer") final Function<Tournament, TournamentSummary> tournamentToSummaryTransformer) {
        notNull(tournamentHost, "Tournament Host may not be null");
        notNull(tournamentSummaryRepository, "Tournament Summary Repository may not be null");
        notNull(tournamentToSummaryTransformer, "tournamentToSummaryTransformer cannot be null");

        this.tournamentHost = tournamentHost;
        this.tournamentSummaryRepository = tournamentSummaryRepository;
        this.tournamentToSummaryTransformer = tournamentToSummaryTransformer;
    }

    private void checkForInitialisation() {
        if (tournamentHost == null || tournamentSummaryRepository == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public Tournament eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public Object[] process(final Tournament tournament) {
        LOG.debug("Settling tournament: {}", tournament);

        try {
            checkForInitialisation();
            notNull(tournament, "Tournament may not be null");
        } catch (Exception e) {
            LOG.error("Internal error", e);
            return new Object[]{tournament};
        }

        final List<Object> objectsToWrite = new ArrayList<Object>();
        objectsToWrite.add(tournament);

        try {
            tournament.settle(tournamentHost);

            final TournamentSummary summary = tournamentToSummaryTransformer.apply(tournament);
            tournamentSummaryRepository.save(summary);

            final AwardMedalsRequest awardMedalsRequest = new AwardMedalsRequest(
                    tournament.getTournamentId(), summary.getGameType());
            objectsToWrite.add(awardMedalsRequest);

        } catch (Exception e) {
            LOG.error("Tournament {}: Settlement could not be processed", tournament.getTournamentId(), e);
            tournament.setTournamentStatus(TournamentStatus.ERROR);

            tournament.setMonitoringMessage("Error: " + stackTraceOf(e));
        }

        tournamentHost.getTournamentRepository().save(tournament, true);
        return objectsToWrite.toArray(new Object[objectsToWrite.size()]);
    }

    private String stackTraceOf(final Exception e) {
        final StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    void setTournamentToSummaryTransformer(
            final Function<Tournament, TournamentSummary> tournamentToSummaryTransformer) {
        this.tournamentToSummaryTransformer = tournamentToSummaryTransformer;
    }
}
