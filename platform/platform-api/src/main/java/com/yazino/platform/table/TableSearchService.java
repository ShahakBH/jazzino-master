package com.yazino.platform.table;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Describes a service that is responsible for finding tables for a player.
 * todo should this also be responsible for making reservations?
 */
public interface TableSearchService {

    /**
     * Returns a set of tables matching the specified criteria and that the player is not playing at.
     * @param playerId the requesting player, never null.
     * @param tableSearchCriteria the criteria, never null.
     * @return a collection of matches, never null
     */
    Collection<TableSearchResult> findTables(BigDecimal playerId, TableSearchCriteria tableSearchCriteria);

}
