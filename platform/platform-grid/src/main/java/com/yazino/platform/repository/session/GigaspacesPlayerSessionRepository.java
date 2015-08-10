package com.yazino.platform.repository.session;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.ReadModifiers;
import com.yazino.platform.grid.BatchQueryHelper;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.GlobalPlayerListUpdateRequest;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.session.PlayerSessionStatus;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerSessionRepository")
public class GigaspacesPlayerSessionRepository implements PlayerSessionRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspacesPlayerSessionRepository.class);

    private static final SessionPlayerIdComparator PLAYER_ID_COMPARATOR = new SessionPlayerIdComparator();
    private static final int HALF_A_SECOND = 500;
    private static final int TEN_MINUTES = 10;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int ONE_MINUTE_IN_MS = 60 * 1000;

    private int sessionTimeoutMinutes = TEN_MINUTES;
    private long timeout = HALF_A_SECOND;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final Executor executor;

    @Autowired
    public GigaspacesPlayerSessionRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                             @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                             final Routing routing,
                                             final Executor executor) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");
        notNull(executor, "executor may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.executor = executor;
    }

    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }

    public void setSessionTimeoutMinutes(final int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    private GigaSpace spaceFor(final BigDecimal playerId) {
        if (routing.isRoutedToCurrentPartition(playerId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

    @Override
    @Transactional("spaceTransactionManager")
    public void extendCurrentSession(final BigDecimal playerId,
                                     final String sessionKey) {
        notNull(playerId, "playerId may not be null");

        final PlayerSession ps = lock(playerId, sessionKey);
        if (ps != null) {
            localGigaSpace.write(ps, sessionTimeoutMinutes * ONE_MINUTE_IN_MS);
        }
    }

    @Override
    public Collection<PlayerSession> findAllByPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final Set<PlayerSession> sessions = new HashSet<>();
        final PlayerSession[] playerSessions = spaceFor(playerId).readMultiple(new PlayerSession(playerId), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (playerSessions != null) {
            Collections.addAll(sessions, playerSessions);
        }
        return sessions;
    }

    @Override
    public PlayerSession findByPlayerAndSessionKey(final BigDecimal playerId,
                                                   final String sessionKey) {
        notNull(playerId, "playerId may not be null");
        notNull(sessionKey, "sessionKey may not be null");

        return spaceFor(playerId).read(new PlayerSession(playerId, sessionKey), 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    @Transactional("spaceTransactionManager")
    public PlayerSession lock(final BigDecimal playerId,
                              final String sessionKey) {
        notNull(playerId, "playerId may not be null");
        notNull(sessionKey, "sessionKey may not be null");

        if (!routing.isRoutedToCurrentPartition(playerId)) {
            throw new IllegalArgumentException("You cannot lock a player session on another partition: ID = " + playerId);
        }

        final PlayerSession playerSession = localGigaSpace.read(new PlayerSession(playerId, sessionKey), timeout, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (playerSession == null) {
            throw new ConcurrentModificationException("Cannot obtain lock for player ID " + playerId);
        }
        return playerSession;
    }

    @Override
    public Set<BigDecimal> findOnlinePlayers(final Set<BigDecimal> playerIds) {
        notNull(playerIds, "playerIds may not be null");

        return executor.mapReduce(new FindOnlinePlayers(playerIds));
    }

    @Override
    public Map<BigDecimal, PlayerSessionsSummary> findOnlinePlayerSessions(final Set<BigDecimal> playerIds) {
        notNull(playerIds, "playerIds may not be null");

        final BatchQueryHelper<PlayerSession> onlinePlayersQueryHelper = new BatchQueryHelper<>(globalGigaSpace, "playerId");
        final Set<PlayerSession> playerSessions = onlinePlayersQueryHelper.findByIds(PlayerSession.class, new ArrayList<Object>(playerIds));

        final Map<BigDecimal, PlayerSessionsSummary> result = summariseSessions(playerSessions);
        LOG.debug("Retrieved {} onlinePlayerIds for {} playerIds", result.size(), playerIds.size());
        return result;
    }

    private Map<BigDecimal, PlayerSessionsSummary> summariseSessions(final Set<PlayerSession> sessions) {
        final Map<BigDecimal, PlayerSessionsSummary> summary = new HashMap<>();
        for (PlayerSession session : sessions) {
            if (summary.containsKey(session.getPlayerId())) {
                summary.get(session.getPlayerId()).addLocations(session.getLocations());
            } else {
                summary.put(session.getPlayerId(), new PlayerSessionsSummary(
                        session.getNickname(), session.getPictureUrl(), session.getBalanceSnapshot(), session.getLocations()));
            }
        }
        return summary;
    }

    @Override
    public int countPlayerSessions(final boolean onlyPlaying) {
        final PlayerSession playerSession = new PlayerSession();
        if (onlyPlaying) {
            playerSession.setPlaying(true);
        }
        return globalGigaSpace.count(playerSession);
    }

    @Override
    public PagedData<PlayerSession> findAll(final int page) {
        final PlayerSession[] sessions = globalGigaSpace.readMultiple(new PlayerSession(), Integer.MAX_VALUE);
        if (sessions == null || sessions.length == 0) {
            return PagedData.empty();
        }

        Arrays.sort(sessions, PLAYER_ID_COMPARATOR);

        final List<PlayerSession> pagedSessions = new ArrayList<>(pageSize);
        final int startIndex = page * pageSize;
        for (int i = startIndex; i < startIndex + pageSize && i < sessions.length; ++i) {
            pagedSessions.add(sessions[i]);
        }

        return new PagedData<>(startIndex, pagedSessions.size(), sessions.length, pagedSessions);
    }

    @Override
    public boolean isOnline(final BigDecimal playerId) {
        return !findAllByPlayer(playerId).isEmpty();
    }

    @Override
    public void save(final PlayerSession session) {
        notNull(session, "session may not be null");

        spaceFor(session.getPlayerId()).write(session, sessionTimeoutMinutes * ONE_MINUTE_IN_MS);
    }

    @Override
    public void removeAllByPlayer(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        spaceFor(playerId).clear(new PlayerSession(playerId));
    }

    @Override
    public void removeByPlayerAndSessionKey(final BigDecimal playerId,
                                            final String sessionKey) {
        notNull(playerId, "playerId may not be null");
        notNull(sessionKey, "sessionKey may not be null");

        spaceFor(playerId).clear(new PlayerSession(playerId, sessionKey));
    }

    @Override
    public void updateGlobalPlayerList(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        spaceFor(playerId).write(new GlobalPlayerListUpdateRequest(playerId));
    }

    @Override
    public Set<PlayerSessionStatus> findAllSessionStatuses() {
        return executor.mapReduce(new FindPlayerSessionStatuses());
    }

    private static class SessionPlayerIdComparator implements Comparator<PlayerSession>, Serializable {
        private static final long serialVersionUID = -3549150931168285030L;

        @Override
        public int compare(final PlayerSession session1, final PlayerSession session2) {
            return session1.getPlayerId().compareTo(session2.getPlayerId());
        }
    }

    @AutowireTask
    public static class FindPlayerSessionStatuses implements DistributedTask<HashSet<PlayerSessionStatus>, Set<PlayerSessionStatus>> {
        private static final long serialVersionUID = -8685742466785822845L;

        @Resource(name = "gigaSpace")
        private transient GigaSpace localSpace;

        @Override
        public HashSet<PlayerSessionStatus> execute() throws Exception {
            final HashSet<PlayerSessionStatus> result = new HashSet<>();
            for (PlayerSession playerSession : localSpace.readMultiple(new PlayerSession(), Integer.MAX_VALUE)) {
                result.add(new PlayerSessionStatus(playerSession.retrieveLocationIds(), playerSession.getPlayerId()));
            }
            return result;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public Set<PlayerSessionStatus> reduce(final List<AsyncResult<HashSet<PlayerSessionStatus>>> asyncResults) throws Exception {
            final Set<PlayerSessionStatus> result = new HashSet<>();
            for (AsyncResult<HashSet<PlayerSessionStatus>> asyncResult : asyncResults) {
                if (asyncResult.getException() != null) {
                    LOG.error("Find Player Session Statues failed", asyncResult.getException());
                } else {
                    result.addAll(asyncResult.getResult());
                }
            }
            return result;
        }
    }

    @AutowireTask
    public static class FindOnlinePlayers implements DistributedTask<HashSet<BigDecimal>, Set<BigDecimal>> {
        private static final long serialVersionUID = -5845138275375116224L;

        @Resource(name = "gigaSpace")
        private transient GigaSpace localSpace;

        private final Set<BigDecimal> playerIds;

        public FindOnlinePlayers(final Set<BigDecimal> playerIds) {
            notNull(playerIds, "playerIds may not be null");

            this.playerIds = playerIds;
        }

        @Override
        public HashSet<BigDecimal> execute() throws Exception {
            final HashSet<BigDecimal> result = new HashSet<>();

            final BatchQueryHelper<PlayerSession> onlinePlayersQueryHelper = new BatchQueryHelper<>(localSpace, "playerId");
            final Set<PlayerSession> playerSessions = onlinePlayersQueryHelper.findByIds(PlayerSession.class, new ArrayList<Object>(playerIds));
            for (PlayerSession playerSession : playerSessions) {
                result.add(playerSession.getPlayerId());
            }

            return result;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public Set<BigDecimal> reduce(final List<AsyncResult<HashSet<BigDecimal>>> asyncResults) throws Exception {
            final Set<BigDecimal> result = new HashSet<>();
            for (AsyncResult<HashSet<BigDecimal>> asyncResult : asyncResults) {
                if (asyncResult.getException() != null) {
                    LOG.error("Find Online Players failed", asyncResult.getException());
                } else {
                    result.addAll(asyncResult.getResult());
                }
            }
            return result;
        }
    }
}
