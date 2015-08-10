package com.yazino.platform.processor.tournament;

import com.yazino.platform.event.message.TournamentPlayerSummary;
import com.yazino.platform.event.message.TournamentSummaryEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.model.tournament.TournamentSummaryPersistenceRequest;
import com.yazino.platform.persistence.tournament.TournamentSummaryDao;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import org.joda.time.DateTime;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;
import java.util.HashSet;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class TournamentSummaryPersistenceProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentSummaryPersistenceProcessor.class);

    private static final TournamentSummaryPersistenceRequest TEMPLATE = new TournamentSummaryPersistenceRequest();

    private final TournamentSummaryRepository tournamentSummaryRepository;
    private final TournamentSummaryDao tournamentSummaryDao;
    private final GigaSpace tournamentGigaSpace;
    private final QueuePublishingService<TournamentSummaryEvent> eventService;

    /**
     * CGLib constructor.
     */
    TournamentSummaryPersistenceProcessor() {
        this.tournamentSummaryRepository = null;
        this.tournamentSummaryDao = null;
        this.tournamentGigaSpace = null;
        this.eventService = null;
    }

    @Autowired
    public TournamentSummaryPersistenceProcessor(final TournamentSummaryRepository tournamentSummaryRepository,
                                                 final TournamentSummaryDao tournamentSummaryDao,
                                                 @Qualifier("gigaSpace") final GigaSpace tournamentGigaSpace,
                                                 @Qualifier("tournamentSummaryEventQueuePublishingService") final QueuePublishingService<TournamentSummaryEvent> eventService) {
        notNull(tournamentSummaryRepository, "Tournament Summary Repository may not be null");
        notNull(tournamentSummaryDao, "Tournament Summary DAO may not be null");
        notNull(tournamentGigaSpace, "Tournament GigaSpace may not be null");

        this.tournamentSummaryRepository = tournamentSummaryRepository;
        this.tournamentSummaryDao = tournamentSummaryDao;
        this.tournamentGigaSpace = tournamentGigaSpace;
        this.eventService = eventService;
    }

    @EventTemplate
    public TournamentSummaryPersistenceRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public TournamentSummaryPersistenceRequest processRequest(final TournamentSummaryPersistenceRequest request) {
        if (tournamentSummaryDao == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }

        notNull(request, "Request may not be null");
        notNull(request.getTournamentId(), "Request: Tournament ID may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processing persistence request for summary for tournamnet %s",
                    request.getTournamentId()));
        }

        try {
            final TournamentSummary tournamentSummary
                    = tournamentSummaryRepository.findByTournamentId(request.getTournamentId());
            if (tournamentSummary == null) {
                LOG.error("Tournament Summary does not exist: tournament {}", request.getTournamentId());
                tryRemovingMatchingRequests(request);
                return requestInErrorState(request);
            }

            tournamentSummaryDao.save(tournamentSummary);

            eventService.send(convertToEvent(tournamentSummary));

        } catch (Throwable t) {
            LOG.error("Tournament Summary could not be added: tournament {}", request.getTournamentId(), t);
            return requestInErrorState(request);
        }

        tryRemovingMatchingRequests(request);
        return null;
    }

    private TournamentSummaryPersistenceRequest requestInErrorState(final TournamentSummaryPersistenceRequest request) {
        request.setStatus(TournamentSummaryPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private void tryRemovingMatchingRequests(final TournamentSummaryPersistenceRequest request) {
        try {
            final TournamentSummaryPersistenceRequest otherRequestsTemplate = new TournamentSummaryPersistenceRequest(
                    request.getTournamentId());
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
            otherRequestsTemplate.setStatus(TournamentSummaryPersistenceRequest.STATUS_ERROR);
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);

        } catch (Throwable t) {
            LOG.error("Exception removing matching requests see stack trace: ", t);
        }
    }

    private TournamentSummaryEvent convertToEvent(final TournamentSummary tournamentSummary) {
        return new TournamentSummaryEvent(tournamentSummary.getTournamentId(),
                tournamentSummary.getTournamentName(),
                tournamentSummary.getVariationId(),
                tournamentSummary.getGameType(),
                new DateTime(tournamentSummary.getStartDateTime().getTime()),
                new DateTime(tournamentSummary.getFinishDateTime().getTime()),
                playerSummariesFor(tournamentSummary));
    }

    private Collection<TournamentPlayerSummary> playerSummariesFor(final TournamentSummary tournamentSummary) {
        final Collection<TournamentPlayerSummary> playerSummaries = new HashSet<TournamentPlayerSummary>();
        for (com.yazino.platform.tournament.TournamentPlayerSummary summary : tournamentSummary.getPlayers()) {
            playerSummaries.add(new TournamentPlayerSummary(summary.getId(),
                    summary.getLeaderboardPosition(), summary.getPrize()));
        }
        return playerSummaries;
    }
}
