package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultPersistenceRequest;
import net.jini.core.lease.Lease;
import org.joda.time.DateTime;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceTrophyLeaderboardResultRepository implements TrophyLeaderboardResultRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTrophyLeaderboardResultRepository.class);
    private static final int FIVE_SECONDS = 5000;

    private final GigaSpace gigaSpace;

    private int timeOut = FIVE_SECONDS;

    /**
     * CGLib constructor.
     */
    GigaspaceTrophyLeaderboardResultRepository() {
        this.gigaSpace = null;
    }

    @Autowired(required = true)
    public GigaspaceTrophyLeaderboardResultRepository(@Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "GigaSpace may not be null");

        this.gigaSpace = gigaSpace;
    }

    public void setTimeOut(final int timeOut) {
        this.timeOut = timeOut;
    }

    private void checkForInitialisation() {
        if (gigaSpace == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @Override
    public void save(final TrophyLeaderboardResult trophyLeaderboardResult) {
        notNull(trophyLeaderboardResult, "Trophy Leaderboard Result may not be null");

        checkForInitialisation();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Entering save for id %s: %s",
                    trophyLeaderboardResult.getLeaderboardId(), trophyLeaderboardResult));
        }

        gigaSpace.write(trophyLeaderboardResult, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
        final TrophyLeaderboardResultPersistenceRequest persistenceRequest
                = new TrophyLeaderboardResultPersistenceRequest(
                trophyLeaderboardResult.getLeaderboardId(), trophyLeaderboardResult.getResultTime());
        gigaSpace.write(persistenceRequest, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public TrophyLeaderboardResult findByIdAndTime(final BigDecimal trophyLeaderboardId,
                                                   final DateTime resultTime) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");
        notNull(resultTime, "Result Time may not be null");

        checkForInitialisation();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Entering find by id %s and time %s", trophyLeaderboardId, resultTime));
        }

        final TrophyLeaderboardResult resultTemplate = new TrophyLeaderboardResult(trophyLeaderboardId, resultTime);
        return gigaSpace.read(resultTemplate, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public void remove(final BigDecimal trophyLeaderboardResultId,
                       final DateTime resultTime) {
        notNull(trophyLeaderboardResultId, "Trophy Leaderboard Result ID may not be null");
        notNull(resultTime, "Result Time may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Clearing trophy leaderboard result from space: " + trophyLeaderboardResultId + ":" + resultTime);
        }

        final TrophyLeaderboardResult template = new TrophyLeaderboardResult(trophyLeaderboardResultId, resultTime);
        gigaSpace.clear(template);
    }

    @Override
    public Set<TrophyLeaderboardResult> findExpired(final TimeSource timeSource) {
        // TODO write me
        throw new UnsupportedOperationException("Not yet written");
    }
}
