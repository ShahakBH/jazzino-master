package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.statistic.PlayerGameStatistics;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspaceTournamentPlayerStatisticPublisher implements TournamentPlayerStatisticPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentPlayerStatisticPublisher.class);

    private final GigaSpace globalGigaSpace;

    /**
     * CGLib-only constructor.
     */
    public GigaspaceTournamentPlayerStatisticPublisher() {
        this.globalGigaSpace = null;
    }

    @Autowired(required = true)
    public GigaspaceTournamentPlayerStatisticPublisher(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.globalGigaSpace = globalGigaSpace;
    }

    public void publishStatistics(final BigDecimal playerId,
                                  final String gameType,
                                  final Collection<GameStatistic> statistics) {
        LOG.debug("Writing stats for player {}, game {}: {}", playerId, gameType, statistics);

        globalGigaSpace.write(new PlayerGameStatistics(playerId, gameType, statistics));
    }
}
