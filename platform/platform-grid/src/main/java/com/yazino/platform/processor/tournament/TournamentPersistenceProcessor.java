package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPersistenceRequest;
import com.yazino.platform.persistence.tournament.TournamentDao;
import com.yazino.platform.repository.tournament.TournamentRepository;
import org.apache.commons.lang3.ArrayUtils;
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

import java.util.Arrays;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TournamentPersistenceProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentPersistenceProcessor.class);
    private static final TournamentPersistenceRequest TEMPLATE = new TournamentPersistenceRequest();

    private final TournamentRepository tournamentRepository;
    private final TournamentDao tournamentDao;
    private final GigaSpace tournamentGigaSpace;

    /**
     * CGLib constructor.
     */
    TournamentPersistenceProcessor() {
        this.tournamentRepository = null;
        this.tournamentDao = null;
        this.tournamentGigaSpace = null;
    }

    @Autowired
    public TournamentPersistenceProcessor(@Qualifier("tournamentRepository") final TournamentRepository tournamentRepository,
                                          final TournamentDao tournamentDao,
                                          @Qualifier("gigaSpace") final GigaSpace tournamentGigaSpace) {
        this.tournamentGigaSpace = tournamentGigaSpace;
        notNull(tournamentRepository, "Tournament Repository may not be null");
        notNull(tournamentDao, "Tournament DAO may not be null");
        this.tournamentRepository = tournamentRepository;
        this.tournamentDao = tournamentDao;
    }

    @EventTemplate
    public TournamentPersistenceRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    @Transactional
    public TournamentPersistenceRequest processRequest(final TournamentPersistenceRequest request) {
        if (tournamentDao == null || tournamentRepository == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
        notNull(request, "Request may not be null");
        notNull(request.getTournamentId(), "Request: Tournament ID may not be null");
        final TournamentPersistenceRequest[] removed = tryRemovingMatchingRequests(request);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing persistence request for tournament " + request.getTournamentId());
        }
        try {
            final Tournament tournament = tournamentRepository.findById(request.getTournamentId());
            if (tournament == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Persistence request ignored: tournament does not exist in space: "
                            + request.getTournamentId());
                }
                return null;
            }
            tournamentDao.save(tournament);

        } catch (Throwable t) {
            LOG.error("Tournament could not be persisted: " + request.getTournamentId(), t);
            if (removed.length > 0) {
                tournamentGigaSpace.writeMultiple(removed);
            }
            return requestInErrorState(request);
        }
        return null;
    }

    private TournamentPersistenceRequest requestInErrorState(final TournamentPersistenceRequest request) {
        request.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
        return request;
    }

    private TournamentPersistenceRequest[] tryRemovingMatchingRequests(final TournamentPersistenceRequest request) {
        TournamentPersistenceRequest[] removed = new TournamentPersistenceRequest[0];
        try {
            final TournamentPersistenceRequest otherRequestsTemplate
                    = new TournamentPersistenceRequest(request.getTournamentId());
            final TournamentPersistenceRequest[] persistenceRequests = tournamentGigaSpace.takeMultiple(
                    otherRequestsTemplate, Integer.MAX_VALUE);
            removed = ArrayUtils.addAll(removed, persistenceRequests);
            final String msgFormat = "removing tournament persistence requests:%s";
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(msgFormat, Arrays.asList(persistenceRequests)));
            }
            otherRequestsTemplate.setStatus(TournamentPersistenceRequest.STATUS_ERROR);
            final TournamentPersistenceRequest[] errorRequests = tournamentGigaSpace.takeMultiple(
                    otherRequestsTemplate, Integer.MAX_VALUE);
            removed = ArrayUtils.addAll(removed, errorRequests);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(msgFormat, Arrays.asList(errorRequests)));
            }

        } catch (Throwable t) {
            LOG.error("Exception removing matching requests see stack trace: ", t);
        }
        return removed;
    }
}
