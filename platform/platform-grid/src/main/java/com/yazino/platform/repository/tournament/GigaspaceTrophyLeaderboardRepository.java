package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
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
import java.util.*;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("trophyLeaderboardRepository")
public class GigaspaceTrophyLeaderboardRepository implements TrophyLeaderboardRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTrophyLeaderboardRepository.class);

    private static final String CURRENT_AND_ACTIVE_LEADERBOARDS
            = "startTime <= ? AND endTime >= ? AND gameType = ? AND active = true";
    private static final String RESULTING_REQUIRED
            = "active = true AND currentCycleEnd <= ?";
    private static final int FIVE_SECONDS = 5000;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;

    private int timeOut = FIVE_SECONDS;

    /**
     * CGLib constructor.
     */
    GigaspaceTrophyLeaderboardRepository() {
        this.localGigaSpace = null;
        this.globalGigaSpace = null;
        this.routing = null;
    }

    @Autowired
    public GigaspaceTrophyLeaderboardRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                                @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                                final Routing routing) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
    }

    public void setTimeOut(final int timeOut) {
        this.timeOut = timeOut;
    }

    private void checkForInitialisation() {
        if (localGigaSpace == null
                || globalGigaSpace == null
                || routing == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }


    @Override
    public Set<TrophyLeaderboard> findCurrentAndActiveWithGameType(final DateTime currentDate,
                                                                   final String gameType) {
        notNull(currentDate, "Current Date may not be null");
        notBlank(gameType, "Game Type may not be null/empty");

        LOG.debug("Finding current & active leaderboards at {} with game type {}", currentDate, gameType);

        final SQLQuery<TrophyLeaderboard> leaderboardQuery = new SQLQuery<>(
                TrophyLeaderboard.class, CURRENT_AND_ACTIVE_LEADERBOARDS, currentDate, currentDate, gameType);

        return queryAndReturnSet(leaderboardQuery, globalGigaSpace);
    }


    @Override
    public Set<TrophyLeaderboard> findLocalResultingRequired(final TimeSource timeSource) {
        notNull(timeSource, "Time Source may not be null");

        final DateTime currentTime = new DateTime(timeSource.getCurrentTimeStamp());

        LOG.debug("Searching for leaderboards requiring result at {}", currentTime);

        final SQLQuery<TrophyLeaderboard> leaderboardQuery = new SQLQuery<>(
                TrophyLeaderboard.class, RESULTING_REQUIRED, currentTime);

        return queryAndReturnSet(leaderboardQuery, localGigaSpace);
    }

    @Override
    public void save(final TrophyLeaderboard trophyLeaderboard) {
        notNull(trophyLeaderboard, "Trophy Leaderboard may not be null");

        checkForInitialisation();

        LOG.debug("Entering save for id {}: {}", trophyLeaderboard.getId(), trophyLeaderboard);

        final GigaSpace spaceReference = spaceFor(trophyLeaderboard.getId());
        spaceReference.write(trophyLeaderboard, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
        final TrophyLeaderboardPersistenceRequest persistenceRequest = new TrophyLeaderboardPersistenceRequest(
                trophyLeaderboard.getId(), TrophyLeaderboardPersistenceRequest.Operation.SAVE);
        spaceReference.write(persistenceRequest, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void archive(final TrophyLeaderboard trophyLeaderboard) {
        notNull(trophyLeaderboard, "Trophy Leaderboard may not be null");

        checkForInitialisation();

        LOG.debug("Entering archive for id {}: {}", trophyLeaderboard.getId(), trophyLeaderboard);

        final GigaSpace spaceReference = spaceFor(trophyLeaderboard.getId());
        spaceReference.write(trophyLeaderboard, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
        final TrophyLeaderboardPersistenceRequest persistenceRequest = new TrophyLeaderboardPersistenceRequest(
                trophyLeaderboard.getId(), TrophyLeaderboardPersistenceRequest.Operation.ARCHIVE);
        spaceReference.write(persistenceRequest, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void clear(final BigDecimal trophyLeaderboardId) {
        notNull(trophyLeaderboardId, "Trophy Leaderboard ID may not be null");

        checkForInitialisation();

        LOG.debug("Entering clear for id {}", trophyLeaderboardId);
        spaceFor(trophyLeaderboardId).clear(new TrophyLeaderboard(trophyLeaderboardId));
    }


    @Override
    public TrophyLeaderboard[] findAll() {
        final TrophyLeaderboard[] result = globalGigaSpace.readMultiple(new TrophyLeaderboard(), Integer.MAX_VALUE);
        if (result == null) {
            return new TrophyLeaderboard[0];
        }
        return result;
    }

    @Override
    public TrophyLeaderboard findById(final BigDecimal trophyLeaderboardId) {
        checkForInitialisation();

        LOG.debug("Entering find by id {}", trophyLeaderboardId);

        return readById(trophyLeaderboardId, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public TrophyLeaderboard lock(final BigDecimal trophyLeaderboardId) {
        checkForInitialisation();

        LOG.debug("Entering lock for id {}", trophyLeaderboardId);

        final TrophyLeaderboard trophyLeaderboard = readById(
                trophyLeaderboardId, timeOut, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (trophyLeaderboard == null) {
            throw new ConcurrentModificationException(
                    "Cannot obtain lock, will reprocess lock for trophy leaderboard: " + trophyLeaderboardId);
        }

        return trophyLeaderboard;
    }

    @Override
    public TrophyLeaderboard findByGameType(final String gameType) {
        checkForInitialisation();

        final TrophyLeaderboard template = new TrophyLeaderboard();
        template.setGameType(gameType);
        final TrophyLeaderboard found = globalGigaSpace.read(template);
        LOG.debug("TrophyLeaderboard was [{}] for gameType [{}]", found, gameType);
        if (found == null) {
            return template;
        }
        return found;
    }

    @Override
    public void requestUpdate(final BigDecimal id,
                              final BigDecimal tournamentId,
                              final BigDecimal playerId,
                              final String playerName,
                              final String playerPictureUrl,
                              final int leaderBoardPosition,
                              final int tournamentPlayerCount) {
        notNull(id, "id may not be null");
        notNull(tournamentId, "tournamentId may not be null");
        notNull(playerId, "playerId may not be null");

        spaceFor(id).write(new TrophyLeaderboardPlayerUpdateRequest(id, tournamentId, playerId, playerName, playerPictureUrl,
                leaderBoardPosition, tournamentPlayerCount));
    }

    private TrophyLeaderboard readById(final BigDecimal id, final long timeout, final ReadModifiers modifiers) {
        return spaceFor(id).readById(TrophyLeaderboard.class, id, id, timeout, modifiers);
    }

    private Set<TrophyLeaderboard> queryAndReturnSet(final SQLQuery<TrophyLeaderboard> leaderboardQuery,
                                                     final GigaSpace spaceReference) {
        assert leaderboardQuery != null;

        checkForInitialisation();

        final TrophyLeaderboard[] trophyLeaderboards = spaceReference.readMultiple(leaderboardQuery, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (trophyLeaderboards == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(trophyLeaderboards));
    }

    private GigaSpace spaceFor(final Object spaceId) {
        if (routing.isRoutedToCurrentPartition(spaceId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
