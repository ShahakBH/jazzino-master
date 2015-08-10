package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.model.tournament.TournamentSummaryPersistenceRequest;
import com.yazino.platform.persistence.tournament.TournamentSummaryDao;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceTournamentSummaryRepository implements TournamentSummaryRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentSummaryRepository.class);
    private static final int FIVE_SECONDS = 5000;

    private final GigaSpace gigaSpace;
    private final TournamentSummaryDao tournamentSummaryDao;

    private int timeOut = FIVE_SECONDS;

    /**
     * CGLib constructor.
     */
    GigaspaceTournamentSummaryRepository() {
        this.gigaSpace = null;
        this.tournamentSummaryDao = null;
    }

    @Autowired(required = true)
    public GigaspaceTournamentSummaryRepository(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                                final TournamentSummaryDao tournamentSummaryDao) {
        notNull(gigaSpace, "Gigaspace may not be null");
        notNull(tournamentSummaryDao, "tournamentSummaryDao may not be null");

        this.gigaSpace = gigaSpace;
        this.tournamentSummaryDao = tournamentSummaryDao;
    }

    private void checkForInitialisation() {
        if (gigaSpace == null
                || tournamentSummaryDao == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    public void setTimeOut(final int timeOut) {
        this.timeOut = timeOut;
    }

    private TournamentSummary readById(final BigDecimal id,
                                       final long timeout,
                                       final ReadModifiers modifiers) {
        return gigaSpace.readById(TournamentSummary.class, id, id, timeout, modifiers);
    }

    private TournamentSummary dirtyRead(final BigDecimal tournamentId) {
        return readById(tournamentId, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public TournamentSummary findMostRecent(final String gameType) {
        notBlank(gameType, "Game Type may not be blank");

        checkForInitialisation();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering find most recent");
        }

        final SQLQuery<TournamentSummary> mostRecentQuery = new SQLQuery<TournamentSummary>(TournamentSummary.class,
                "finishDateTime IS NOT NULL AND gameType=? ORDER BY finishDateTime DESC", gameType);
        return gigaSpace.readIfExists(mostRecentQuery, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public TournamentSummary findByTournamentId(final BigDecimal tournamentId) {
        checkForInitialisation();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering find by id: " + tournamentId);
        }

        notNull(tournamentId, "Tournament ID may not be null");

        return dirtyRead(tournamentId);
    }

    @Override
    public void save(final TournamentSummary summary) {
        checkForInitialisation();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Entering save: " + summary);
        }

        notNull(summary, "Summary may not be null");

        gigaSpace.write(summary, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
        gigaSpace.write(new TournamentSummaryPersistenceRequest(summary.getTournamentId()));
    }

    @Override
    public void delete(final BigDecimal tournamentId) {
        checkForInitialisation();

        notNull(tournamentId, "tournamentId may not be null");

        tournamentSummaryDao.delete(tournamentId);
    }
}
