package com.yazino.platform.community;

import com.yazino.platform.tournament.TrophyWinner;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides Trophy services.
 */
public interface PlayerTrophyService {

    /**
     * Finds all trophies that the specified player has.
     *
     * @param gameType    the game type, never null
     * @param playerId    the player if, never null
     * @param trophyNames collection of trophy names for which to search
     * @return a collection of player trophies, never null
     */
    TrophyCabinet findTrophyCabinetForPlayer(String gameType,
                                             @Routing BigDecimal playerId,
                                             Collection<String> trophyNames);

    /**
     * Returns a collection of winners of a particular trophy.
     *
     * @param trophyName          the trophy name
     * @param maxResultsForTrophy the number of winners to retrieve
     * @param gameTypes           the game
     * @return a map of winners, never null
     */
    Map<String, List<TrophyWinner>> findWinnersByTrophyName(String trophyName,
                                                            int maxResultsForTrophy,
                                                            String... gameTypes);

}
