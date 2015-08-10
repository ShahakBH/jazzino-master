package com.yazino.platform.repository.community;

import com.yazino.platform.model.community.PlayerTrophy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * Manages a repository of Player Trophy objects.
 */
public interface PlayerTrophyRepository {

    void save(PlayerTrophy playerTrophy);

    Collection<PlayerTrophy> findPlayersTrophies(BigDecimal playerId);

    List<PlayerTrophy> findWinnersByTrophyId(BigDecimal trophyId, int maxResults);

}
