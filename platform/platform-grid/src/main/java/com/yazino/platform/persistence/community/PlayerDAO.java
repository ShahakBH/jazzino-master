package com.yazino.platform.persistence.community;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;

public interface PlayerDAO {

    void save(Player p);

    Player findById(BigDecimal playerId);

    Collection<Player> findByIds(Set<BigDecimal> playerIds);

    Set<Player> findAll();

    /**
     * Find all players with a name starting with the search string.
     *
     * @param name              the name to search for.
     * @param page              the page of results to return, where 0 is the first page.
     * @param playerIdToExclude if non-null, this player should be excluded from the results.
     * @return the results.
     */

    PagedData<Player> findByName(String name, int page, BigDecimal playerIdToExclude);

    void updateLastPlayedTs(BigDecimal playerId, DateTime lastPlayedTs);
}
