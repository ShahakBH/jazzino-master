package com.yazino.game.api.statistic;


import com.yazino.game.api.GamePlayer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * A game specific strategy that consumes statistics to produce an outcome.
 * <p/>
 * For instance, the outcome may be notifications, tracking data, achievements, and so on.
 */
public interface GameStatisticConsumer {

    /**
     * Consume the statistics for a given player.
     *
     *
     * @param player           the player the statistics are associated with.
     * @param tableId          the table id.
     * @param gameType
     *@param clientProperties the client properties for the client that produced the statistics.
     * @param statistics       the statistics.   @return relevant events for the player.
     */

    Set<StatisticEvent> consume(GamePlayer player,
                                BigDecimal tableId,
                                final String gameType, Map<String, String> clientProperties,
                                GameStatistics statistics);

    /**
     * Decides if consumer can work with a given game type.
     *
     * @param gameType the game type
     * @return if can consume
     */
    boolean acceptsGameType(String gameType);
}
