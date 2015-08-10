package com.yazino.platform.test;

import com.yazino.platform.model.community.*;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Repository("playerRepository")
public class InMemoryPlayerRepository implements PlayerRepository {

    private final Map<BigDecimal, Player> players = new HashMap<>();

    @Override
    public Player findById(final BigDecimal playerId) {
        return players.get(playerId);
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
        final Player player = findById(playerId);
        if (player != null) {
            return new PlayerSessionSummary(playerId, player.getAccountId(), player.getName(), null);
        }
        return null;
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
        return findById(playerId);
    }

    @Override
    public void savePublishStatusRequest(final PublishStatusRequest request) {
    }

    @Override
    public void requestLastPlayedUpdates(final PlayerLastPlayedUpdateRequest[] updateRequests) {

    }

    @Override
    public void requestRelationshipChanges(final Set<RelationshipActionRequest> requests) {

    }

    @Override
    public void publishFriendsSummary(final BigDecimal playerId) {

    }

    @Override
    public void requestFriendRegistration(final BigDecimal playerId, final Set<BigDecimal> friendIds) {

    }

    @Override
    public void addTag(final BigDecimal playerId, final String tag) {
        final Player player = findById(playerId);
        if (player != null) {
            if (player.getTags() == null) {
                player.setTags(new HashSet<String>());
            }
            player.getTags().add(tag);
        }
    }

    @Override
    public void removeTag(final BigDecimal playerId, final String tag) {
        final Player player = findById(playerId);
        if (player != null) {
            if (player.getTags() == null) {
                player.setTags(new HashSet<String>());
            }
            player.getTags().remove(tag);
        }
    }

    public Player findByName(final String name) {
        for (Player player : players.values()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }
}
