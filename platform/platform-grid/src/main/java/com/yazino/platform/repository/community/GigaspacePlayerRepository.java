package com.yazino.platform.repository.community;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.BatchQueryHelper;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.SpaceAccess;
import com.yazino.platform.model.community.*;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.persistence.community.PlayerDAO;
import com.yazino.platform.processor.community.PlayerLastPlayedPersistenceRequest;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.util.BigDecimals;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.model.community.PlayerTaggingRequest.Action.ADD;
import static com.yazino.platform.model.community.PlayerTaggingRequest.Action.REMOVE;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerRepository")
public class GigaspacePlayerRepository implements PlayerRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspacePlayerRepository.class);

    private static final int REQUEST_TIMEOUT = 30000;
    private static final int FIVE_SECONDS = 5000;
    private static final long ONE_SECOND = 1000L;

    private int timeout = FIVE_SECONDS;

    private final SpaceAccess space;
    private final PlayerDAO playerDAO;
    private final Executor executor;

    @Autowired
    public GigaspacePlayerRepository(final SpaceAccess space,
                                     final PlayerDAO playerDAO,
                                     final Executor executor) {
        notNull(space, "space may not be null");
        notNull(playerDAO, "playerDAO may not be null");
        notNull(executor, "executor may not be null");

        this.space = space;
        this.playerDAO = playerDAO;
        this.executor = executor;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public Player findById(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final GigaSpace spaceReference = space.forRouting(playerId);
        Player player = spaceReference.readById(Player.class, playerId, playerId, 0, ReadModifiers.DIRTY_READ);

        if (player == null) {
            player = playerDAO.findById(playerId);
            if (player != null) {
                spaceReference.write(player, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
            }
        }

        return player;
    }

    @SuppressWarnings("unchecked")
    public Set<Player> findLocalByIds(final Set<BigDecimal> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<Player> matchingPlayers = new HashSet();

        final Set<Player> playersFromSpace = new BatchQueryHelper<Player>(space.local(), "playerId")
                .findByIds(Player.class, (Collection) playerIds);

        final Set<BigDecimal> idsToLoadFromDatabase = new HashSet<>();
        for (BigDecimal playerId : playerIds) {
            if (!hasPlayerWithId(playersFromSpace, playerId) && space.isRoutedLocally(playerId)) {
                idsToLoadFromDatabase.add(playerId);
            }
        }

        if (!idsToLoadFromDatabase.isEmpty()) {
            final Collection<Player> playersNotInSpace = playerDAO.findByIds(idsToLoadFromDatabase);
            if (!playersNotInSpace.isEmpty()) {
                space.local().writeMultiple(playersNotInSpace.toArray(new Player[playersNotInSpace.size()]));
                matchingPlayers.addAll(playersNotInSpace);
            }
        }

        matchingPlayers.addAll(playersFromSpace);
        return matchingPlayers;
    }

    private boolean hasPlayerWithId(final Set<Player> players, final BigDecimal playerId) {
        for (Player player : players) {
            if (BigDecimals.equalByComparison(player.getPlayerId(), playerId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PlayerSessionSummary findSummaryByPlayerAndSession(final BigDecimal playerId, final BigDecimal sessionId) {
        notNull(playerId, "playerId may not be null");

        return executor.remoteExecute(new FindPlayerSessionSummaryTask(playerId, sessionId), playerId);
    }

    @Override
    public Player lock(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        LOG.debug("Locking {}", playerId);

        if (!space.isRoutedLocally(playerId)) {
            throw new IllegalArgumentException("You cannot lock a player on another partition: ID = " + playerId);
        }

        final Player p = space.local().readById(Player.class, playerId, playerId, timeout, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (p == null) {
            throw new ConcurrentModificationException("Cannot obtain lock for player ID " + playerId);
        }
        return p;
    }

    @Override
    public void save(final Player player) {
        notNull(player, "player may not be null");

        LOG.debug("Saving {}", player);

        space.forRouting(player.getPlayerId()).writeMultiple(new Object[]{player, new PlayerPersistenceRequest(player.getPlayerId())},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void savePublishStatusRequest(final PublishStatusRequest request) {
        notNull(request, "request may not be null");

        LOG.debug("Saving PublishStatusRequest {}", request);

        space.forRouting(request.getPlayerId()).write(request, REQUEST_TIMEOUT);
    }

    @Override
    public void saveLastPlayed(final Player player) {
        notNull(player, "player may not be null");

        LOG.debug("Saving last played for {}", player);

        space.forRouting(player.getPlayerId()).writeMultiple(new Object[]{player, new PlayerLastPlayedPersistenceRequest(player.getPlayerId())},
                Lease.FOREVER, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void requestLastPlayedUpdates(final PlayerLastPlayedUpdateRequest[] updateRequests) {
        for (PlayerLastPlayedUpdateRequest entry : updateRequests) {
            saveLastPlayedUpdateRequest(entry);
        }
    }

    @Override
    public void requestRelationshipChanges(final Set<RelationshipActionRequest> requests) {
        notNull(requests, "requests may not be null");

        if (!requests.isEmpty()) {
            space.global().writeMultiple(requests.toArray(new RelationshipActionRequest[requests.size()]));
        }
    }

    @Override
    public void publishFriendsSummary(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        space.forRouting(playerId).write(new FriendsSummaryRequest(playerId),
                Lease.FOREVER, ONE_SECOND, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void requestFriendRegistration(final BigDecimal playerId,
                                          final Set<BigDecimal> friendIds) {
        notNull(playerId, "playerId may not be null");
        notNull(friendIds, "friendIds may not be null");

        if (friendIds.isEmpty()) {
            return;
        }

        space.forRouting(playerId).write(new FriendRegistrationRequest(playerId, friendIds));
    }

    @Override
    public void addTag(final BigDecimal playerId, final String tag) {
        notNull(playerId, "playerId may not be null");
        notNull(tag, "tag may not be null");

        space.forRouting(playerId).write(new PlayerTaggingRequest(playerId, tag, ADD));
    }

    @Override
    public void removeTag(final BigDecimal playerId, final String tag) {
        notNull(playerId, "playerId may not be null");
        notNull(tag, "tag may not be null");

        space.forRouting(playerId).write(new PlayerTaggingRequest(playerId, tag, REMOVE));
    }

    private void saveLastPlayedUpdateRequest(final PlayerLastPlayedUpdateRequest entry) {
        LOG.debug("Writing PlayerLastPlayedUpdateRequest to space: {}", entry);
        space.forRouting(entry.getPlayerId()).write(entry);
    }

    @AutowireTask
    public static class FindPlayerSessionSummaryTask implements Task<PlayerSessionSummary> {
        private static final long serialVersionUID = -6373102844395242931L;

        @Resource(name = "playerRepository")
        private transient PlayerRepository playerRepository;
        @Resource(name = "playerSessionRepository")
        private transient PlayerSessionRepository playerSessionRepository;

        private final BigDecimal playerId;
        private final BigDecimal sessionId;

        public FindPlayerSessionSummaryTask(final BigDecimal playerId,
                                            final BigDecimal sessionId) {
            this.playerId = playerId;
            this.sessionId = sessionId;
        }

        @Override
        public PlayerSessionSummary execute() throws Exception {
            final Player player = playerRepository.findById(playerId);
            if (player == null) {
                return null;
            }
            return new PlayerSessionSummary(player.getPlayerId(), player.getAccountId(), player.getName(), sessionId());
        }

        private BigDecimal sessionId() {
            if (sessionId != null) {
                return sessionId;
            }

            final Collection<PlayerSession> sessions = playerSessionRepository.findAllByPlayer(playerId);
            if (!sessions.isEmpty()) {
                return sessions.iterator().next().getSessionId();
            }

            return null;
        }
    }

}
