package com.yazino.host.community;

import com.yazino.host.session.StandalonePlayerSessionRepository;
import com.yazino.host.table.StandaloneTableInviteRepository;
import com.yazino.host.table.document.StandaloneDocumentDispatcher;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.model.community.*;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.community.PlayerWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class StandalonePlayerRepository implements PlayerRepository {
    private final StandaloneDocumentDispatcher documentDispatcher;
    private final StandalonePlayerSessionRepository playerSessionRepository;
    private final StandaloneTableInviteRepository tableInviteRepository;
    private final Map<BigDecimal, Player> players = new HashMap<>();
    private PlayerWorker playerWorker = new PlayerWorker();

    @Autowired
    public StandalonePlayerRepository(
            @Qualifier("standaloneDocumentDispatcher") final StandaloneDocumentDispatcher documentDispatcher,
            final StandalonePlayerSessionRepository playerSessionRepository,
            final StandaloneTableInviteRepository tableInviteRepository) {
        this.documentDispatcher = documentDispatcher;
        this.playerSessionRepository = playerSessionRepository;
        this.tableInviteRepository = tableInviteRepository;
    }

    @Override
    public Player findById(final BigDecimal playerId) {
        final Player player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player " + playerId + " not found!");
        }
        return player;
    }

    @Override
    public Set<Player> findLocalByIds(final Set<BigDecimal> playerIds) {
        final Set<Player> matchingPlayers = new HashSet<>();
        for (BigDecimal playerId : playerIds) {
            if (players.containsKey(playerId)) {
                matchingPlayers.add(players.get(playerId));
            }
        }
        return matchingPlayers;
    }

    @Override
    public PlayerSessionSummary findSummaryByPlayerAndSession(final BigDecimal playerId, final BigDecimal sessionId) {
        final Player player = players.get(playerId);
        if (player == null) {
            return null;
        }

        final Collection<PlayerSession> session = playerSessionRepository.findAllByPlayer(playerId);
        if (session.isEmpty()) {
            return new PlayerSessionSummary(playerId, player.getAccountId(), player.getName(), null);
        }

        return new PlayerSessionSummary(playerId, player.getAccountId(), player.getName(), session.iterator().next().getSessionId());
    }

    @Override
    public void save(final Player player) {
        players.put(player.getPlayerId(), player);
    }

    @Override
    public void saveLastPlayed(final Player player) {
        players.put(player.getPlayerId(), player);
    }

    @Override
    public Player lock(final BigDecimal playerId) {
        return players.get(playerId);
    }

    @Override
    public void requestLastPlayedUpdates(final PlayerLastPlayedUpdateRequest[] updateRequests) {

    }

    @Override
    public void requestRelationshipChanges(final Set<RelationshipActionRequest> requests) {

    }

    @Override
    public void requestFriendRegistration(final BigDecimal playerId, final Set<BigDecimal> friendIds) {

    }

    @Override
    public void publishFriendsSummary(final BigDecimal playerId) {

    }

    @Override
    public void addTag(final BigDecimal playerId, final String tag) {
        final Player player = players.get(playerId);
        if (player != null) {
            if (player.getTags() == null) {
                player.setTags(new HashSet<String>());
            }
            player.getTags().add(tag);
        }
    }

    @Override
    public void removeTag(final BigDecimal playerId, final String tag) {
        final Player player = players.get(playerId);
        if (player != null) {
            if (player.getTags() == null) {
                player.setTags(new HashSet<String>());
            }
            player.getTags().remove(tag);
        }
    }

    @Override
    public void savePublishStatusRequest(final PublishStatusRequest publishStatusRequest) {
        if (PublishStatusRequestType.RELATIONSHIPS != publishStatusRequest.getRequestType()) {
            throw new UnsupportedOperationException("Can't process request " + publishStatusRequest);
        }
        final Player player = findById(publishStatusRequest.getPlayerId());
        final Document document = playerWorker.buildRelationshipDocument(player, playerSessionRepository, tableInviteRepository);
        documentDispatcher.dispatch(document, player.getPlayerId());
    }
}
