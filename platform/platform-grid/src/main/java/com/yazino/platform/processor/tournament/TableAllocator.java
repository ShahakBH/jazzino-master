package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;

import java.util.Collection;

/**
 * Interface for the logic to assign tournaments participants to tables.
 */
public interface TableAllocator {

    /**
     * Get a unique ID that identifies this allocator.
     *
     * @return the ID for this allocator.
     */
    String getId();

    /**
     * Allocate the given players to tables.
     *
     * @param players   the players to allocate.
     * @param tableSize the table size.
     * @return a collection of player groups, each group being intended for a single table.
     */
    Collection<PlayerGroup> allocate(Collection<TournamentPlayer> players, int tableSize);

}
