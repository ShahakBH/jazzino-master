package com.yazino.platform.processor.table;

import com.yazino.platform.model.statistic.PlayerGameStatistics;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;
import org.apache.commons.lang3.ArrayUtils;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SpaceGameStatisticsPublisher implements GameStatisticsPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(SpaceGameStatisticsPublisher.class);
    private final GigaSpace gigaSpace;

    @Autowired
    public SpaceGameStatisticsPublisher(@Qualifier("globalGigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "gigaSpace is null");
        this.gigaSpace = gigaSpace;
    }

    @Override
    public void publish(final BigDecimal tableId,
                        final String gameType,
                        final String clientId,
                        final Collection<GameStatistic> statistics) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("entering publish");
        }
        if (statistics == null) {
            return;
        }
        final Map<BigDecimal, Collection<GameStatistic>> playerStats
                = new HashMap<BigDecimal, Collection<GameStatistic>>();
        for (GameStatistic statistic : statistics) {
            final BigDecimal playerId = statistic.getPlayerId();
            Collection<GameStatistic> statisticsForPlayer = playerStats.get(playerId);
            if (statisticsForPlayer == null) {
                statisticsForPlayer = new HashSet<GameStatistic>();
                playerStats.put(playerId, statisticsForPlayer);
            }
            statisticsForPlayer.add(statistic);
        }

        if (playerStats.size() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("publishing %s game statistics", playerStats.size()));
            }
            final PlayerGameStatistics[] gamePlayerStatistics = new PlayerGameStatistics[playerStats.size()];
            int count = 0;
            for (BigDecimal playerId : playerStats.keySet()) {
                gamePlayerStatistics[count] = new PlayerGameStatistics(
                        playerId, tableId, gameType, clientId, playerStats.get(playerId));
                count++;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Writing statistics to space:  " + ArrayUtils.toString(gamePlayerStatistics));
            }
            gigaSpace.writeMultiple(gamePlayerStatistics);

        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Processor produced no statistics");
        }
    }
}
