package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.apache.commons.lang3.Validate.notEmpty;

/**
 * Should allocate players to tables evenly and ordered by tournament account balance.
 */
@Service
@Qualifier("tableAllocator")
public class EvenByBalanceTableAllocator implements TableAllocator {

    private static final int MINIMUM_TABLE_SIZE = 1;
    private static final String ALLOCATOR_ID = "EVEN_BY_BALANCE";

    @Override
    public String getId() {
        return ALLOCATOR_ID;
    }

    public Collection<PlayerGroup> allocate(
            final Collection<TournamentPlayer> playersToAllocate, final int tableSize) {
        notEmpty(playersToAllocate, "Players may not be null or empty");
        if (tableSize < MINIMUM_TABLE_SIZE) {
            throw new IllegalArgumentException("Table size must be >= " + MINIMUM_TABLE_SIZE);
        }

        final int numberOfTables = numberOfTables(playersToAllocate, tableSize);

        final int averagePlayersAtTable = playersToAllocate.size() / numberOfTables;
        int overflow = playersToAllocate.size() % numberOfTables;

        final List<TournamentPlayer> unallocatedPlayers = new LinkedList<TournamentPlayer>(playersToAllocate);
        Collections.sort(unallocatedPlayers);

        final Collection<PlayerGroup> allocatedTables = new ArrayList<PlayerGroup>();

        for (int tableNumber = 0; tableNumber < numberOfTables; ++tableNumber) {
            final PlayerGroup playersForTable = new PlayerGroup();

            int playersAtTable = averagePlayersAtTable;
            if (overflow > 0) {
                ++playersAtTable;
                --overflow;
            }
            for (int playerNumber = 0; playerNumber < playersAtTable && unallocatedPlayers.size() > 0;
                 ++playerNumber) {
                playersForTable.add(unallocatedPlayers.remove(0));
            }

            allocatedTables.add(playersForTable);
        }

        assert unallocatedPlayers.size() == 0 : "Not all players were allocated";

        return allocatedTables;
    }

    private int numberOfTables(final Collection<TournamentPlayer> playersToAllocate, final int tableSize) {
        final int baseSize = playersToAllocate.size() / tableSize;
        if ((playersToAllocate.size() % tableSize) > 0) {
            return baseSize + 1;
        }
        return baseSize;
    }
}
