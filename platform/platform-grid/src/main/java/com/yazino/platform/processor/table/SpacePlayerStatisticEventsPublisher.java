package com.yazino.platform.processor.table;

import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SpacePlayerStatisticEventsPublisher implements PlayerStatisticEventsPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(SpacePlayerStatisticEventsPublisher.class);
    private final GigaSpace gigaSpace;

    @Autowired
    public SpacePlayerStatisticEventsPublisher(@Qualifier("globalGigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "gigaSpace is null");
        this.gigaSpace = gigaSpace;
    }

    @Override
    public void publishEvents(final Collection<PlayerStatisticEvent> events) {
        LOG.debug("entering publishEvents");

        if (events != null && events.size() > 0) {
            LOG.debug("publishing {}", events);
            gigaSpace.writeMultiple(events.toArray());
        }
    }
}
