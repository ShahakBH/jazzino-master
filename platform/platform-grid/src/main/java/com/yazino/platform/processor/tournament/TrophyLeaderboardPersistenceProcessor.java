package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentPersistenceRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest;
import com.yazino.platform.persistence.tournament.TrophyLeaderboardDao;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TrophyLeaderboardPersistenceProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboardPersistenceProcessor.class);
    private static final TrophyLeaderboardPersistenceRequest TEMPLATE = new TrophyLeaderboardPersistenceRequest();

    private final TrophyLeaderboardRepository trophyLeaderboardRepository;
    private final TrophyLeaderboardDao trophyLeaderboardDao;
    private final GigaSpace tournamentGigaSpace;

    /**
     * CGLib constructor.
     */
    TrophyLeaderboardPersistenceProcessor() {
        this.trophyLeaderboardRepository = null;
        this.trophyLeaderboardDao = null;
        this.tournamentGigaSpace = null;
    }

    @Autowired(required = true)
    public TrophyLeaderboardPersistenceProcessor(@Qualifier("trophyLeaderboardRepository") final TrophyLeaderboardRepository trophyLeaderboardRepository,
                                                 final TrophyLeaderboardDao trophyLeaderboardDao,
                                                 @Qualifier("gigaSpace") final GigaSpace tournamentGigaSpace) {
        notNull(trophyLeaderboardRepository, "Trophy Leaderboard Repository may not be null");
        notNull(trophyLeaderboardDao, "Trophy Leaderboard DAO may not be null");
        notNull(tournamentGigaSpace, "Tournament Gigaspace DAO may not be null");

        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
        this.trophyLeaderboardDao = trophyLeaderboardDao;
        this.tournamentGigaSpace = tournamentGigaSpace;
    }

    @EventTemplate
    public TrophyLeaderboardPersistenceRequest template() {
        return TEMPLATE;
    }

    private void checkForInitialisation() {
        if (trophyLeaderboardDao == null
                || trophyLeaderboardRepository == null
                || tournamentGigaSpace == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @SpaceDataEvent
    @Transactional
    public TrophyLeaderboardPersistenceRequest processRequest(final TrophyLeaderboardPersistenceRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("ProcessRequest: %s", request.toString()));
        }

        try {
            checkForInitialisation();

            notNull(request, "Request may not be null");
            notNull(request.getTrophyLeaderboardId(), "Trophy Leaderboard ID may not be null");

        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
            return null;
        }

        try {
            final TrophyLeaderboard trophyLeaderboard
                    = trophyLeaderboardRepository.findById(request.getTrophyLeaderboardId());
            if (trophyLeaderboard == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Persistence request ignored: trophy leaderboard does not exist in space: "
                            + request.getTrophyLeaderboardId());
                }
                tryRemovingMatchingRequests(request);
                return null;
            }

            switch (request.getOperation()) {
                case SAVE:
                    trophyLeaderboardDao.save(trophyLeaderboard);
                    break;

                case ARCHIVE:
                    trophyLeaderboardDao.save(trophyLeaderboard);
                    trophyLeaderboardRepository.clear(trophyLeaderboard.getId());
                    break;

                default:
                    throw new IllegalArgumentException("Unknown operation: " + request.getOperation()
                            + " for request " + request);
            }

            // Send

        } catch (Throwable t) {
            LOG.error("Trophy Leaderboard could not be persisted: " + request.getTrophyLeaderboardId(), t);
            return requestInErrorState(request);
        }

        tryRemovingMatchingRequests(request);
        return null;
    }

    private TrophyLeaderboardPersistenceRequest requestInErrorState(final TrophyLeaderboardPersistenceRequest request) {
        request.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private void tryRemovingMatchingRequests(final TrophyLeaderboardPersistenceRequest request) {
        try {
            final TrophyLeaderboardPersistenceRequest otherRequestsTemplate
                    = new TrophyLeaderboardPersistenceRequest(request.getTrophyLeaderboardId());
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
            otherRequestsTemplate.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);

        } catch (Throwable t) {
            LOG.error("Exception removing matching requests for " + request.getTrophyLeaderboardId(), t);
        }
    }
}
