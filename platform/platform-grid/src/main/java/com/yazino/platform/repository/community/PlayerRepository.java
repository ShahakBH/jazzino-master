package com.yazino.platform.repository.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerSessionSummary;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.processor.community.PlayerStatusPublisher;
import com.yazino.platform.processor.table.PlayerLastPlayedUpdateRequest;

import java.math.BigDecimal;
import java.util.Set;

public interface PlayerRepository extends PlayerStatusPublisher {
    Player findById(BigDecimal playerId);

    Set<Player> findLocalByIds(Set<BigDecimal> playerIds);

    /**
     * Find a player session summary by player ID and optionally a session ID.
     *
     * @param playerId  the player ID.
     * @param sessionId the session ID. If null, the first session found will be returned.
     * @return the summary.
     */
    PlayerSessionSummary findSummaryByPlayerAndSession(BigDecimal playerId,
                                                       BigDecimal sessionId);

    void save(Player player);

    Player lock(BigDecimal playerId);

    void saveLastPlayed(Player player);

    void requestLastPlayedUpdates(PlayerLastPlayedUpdateRequest[] updateRequests);

    void requestRelationshipChanges(Set<RelationshipActionRequest> requests);

    void requestFriendRegistration(BigDecimal playerId,
                                   Set<BigDecimal> friendIds);

    void publishFriendsSummary(BigDecimal playerId);

    void addTag(BigDecimal playerId, String tag);

    void removeTag(BigDecimal playerId, String tag);
}
