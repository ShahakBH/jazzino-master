package com.yazino.platform.service.statistic;

import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.statistic.PlayerStatistics;
import com.yazino.platform.playerstatistic.StatisticEvent;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Service("statisticEventService")
public class GigaspaceStatisticEventService implements StatisticEventService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceStatisticEventService.class);

    private static final int WRITE_TIMEOUT = 5000;

    private final GigaSpace globalGigaSpace;

    @Autowired
    public GigaspaceStatisticEventService(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.globalGigaSpace = globalGigaSpace;
    }

    @Override
    public void publishStatistics(final BigDecimal playerId,
                                  final String gameType,
                                  final Collection<StatisticEvent> events) {
        notNull(playerId, "playerId may not be null");

        if (events == null || events.isEmpty()) {
            LOG.debug("Events list is empty, ignoring.");
            return;
        }

        LOG.debug("Writing statistics to space for player {} and game type {}: {}", playerId, gameType, events);
        globalGigaSpace.write(new PlayerStatistics(playerId, gameType, events), Lease.FOREVER, WRITE_TIMEOUT, WriteModifiers.UPDATE_OR_WRITE);
    }
}
