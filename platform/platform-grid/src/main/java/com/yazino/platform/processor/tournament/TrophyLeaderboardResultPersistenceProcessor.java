package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentPersistenceRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultPersistenceRequest;
import com.yazino.platform.persistence.tournament.TrophyLeaderboardResultDao;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
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
public class TrophyLeaderboardResultPersistenceProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboardResultPersistenceProcessor.class);
    private static final TrophyLeaderboardResultPersistenceRequest TEMPLATE
            = new TrophyLeaderboardResultPersistenceRequest();

    private final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository;
    private final TrophyLeaderboardResultDao trophyLeaderboardResultDao;
    private final GigaSpace tournamentGigaSpace;

    /**
     * CGLib constructor.
     */
    TrophyLeaderboardResultPersistenceProcessor() {
        this.trophyLeaderboardResultRepository = null;
        this.trophyLeaderboardResultDao = null;
        this.tournamentGigaSpace = null;
    }

    @Autowired(required = true)
    public TrophyLeaderboardResultPersistenceProcessor(
            final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository,
            final TrophyLeaderboardResultDao trophyLeaderboardResultDao,
            @Qualifier("gigaSpace") final GigaSpace tournamentGigaSpace) {
        notNull(trophyLeaderboardResultRepository, "Trophy Leaderboard Result Repository may not be null");
        notNull(trophyLeaderboardResultDao, "Trophy Leaderboard Result DAO may not be null");
        notNull(tournamentGigaSpace, "Tournament Gigaspace DAO may not be null");

        this.trophyLeaderboardResultRepository = trophyLeaderboardResultRepository;
        this.trophyLeaderboardResultDao = trophyLeaderboardResultDao;
        this.tournamentGigaSpace = tournamentGigaSpace;
    }

    @EventTemplate
    public TrophyLeaderboardResultPersistenceRequest template() {
        return TEMPLATE;
    }

    private void checkForInitialisation() {
        if (trophyLeaderboardResultDao == null
                || trophyLeaderboardResultRepository == null
                || tournamentGigaSpace == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @SpaceDataEvent
    @Transactional
    public TrophyLeaderboardResultPersistenceRequest processRequest(
            final TrophyLeaderboardResultPersistenceRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("ProcessRequest: %s", request.toString()));
        }

        try {
            checkForInitialisation();

            notNull(request, "Request may not be null");
            notNull(request.getTrophyLeaderboardId(), "Trophy Leaderboard ID may not be null");
            notNull(request.getResultTime(), "Result Time may not be null");

        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
            return null;
        }

        try {
            final TrophyLeaderboardResult trophyLeaderboardResult = trophyLeaderboardResultRepository.findByIdAndTime(
                    request.getTrophyLeaderboardId(), request.getResultTime());
            if (trophyLeaderboardResult == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Persistence request ignored: trophy leaderboard result does not exist in space: "
                            + request.getTrophyLeaderboardId()
                            + "," + request.getResultTime());
                }
                tryRemovingMatchingRequests(request);
                return null;
            }

            trophyLeaderboardResultDao.save(trophyLeaderboardResult);

        } catch (Throwable t) {
            LOG.error("Trophy Leaderboard could not be persisted: " + request.getTrophyLeaderboardId()
                    + "," + request.getResultTime(), t);
            return requestInErrorState(request);
        }

        tryRemovingMatchingRequests(request);
        return null;
    }

    private TrophyLeaderboardResultPersistenceRequest requestInErrorState(
            final TrophyLeaderboardResultPersistenceRequest request) {
        request.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private void tryRemovingMatchingRequests(final TrophyLeaderboardResultPersistenceRequest request) {
        try {
            final TrophyLeaderboardResultPersistenceRequest otherRequestsTemplate
                    = new TrophyLeaderboardResultPersistenceRequest(
                    request.getTrophyLeaderboardId(), request.getResultTime());
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);
            otherRequestsTemplate.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
            tournamentGigaSpace.takeMultiple(otherRequestsTemplate, Integer.MAX_VALUE);

        } catch (Throwable t) {
            LOG.error("Exception removing matching requests for " + request.getTrophyLeaderboardId()
                    + "," + request.getResultTime(), t);
        }
    }
}
