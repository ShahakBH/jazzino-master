package com.yazino.platform.repository.table;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.table.Countdown;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceCountdownRepository implements CountdownRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceCountdownRepository.class);

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;

    @Autowired
    public GigaspaceCountdownRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                        @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                        final Routing routing) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
    }

    @Override
    public Collection<Countdown> find() {
        final List<Countdown> found = asList(globalGigaSpace.readMultiple(new Countdown(), Integer.MAX_VALUE));
        LOG.debug("Found Countdowns [{}]", found);
        return found;
    }

    @Override
    public Countdown find(final String countdownId) {
        notNull(countdownId, "countdownId may not be null");
        final Countdown found = spaceFor(countdownId).readById(Countdown.class, countdownId);
        LOG.debug("Found Countdown [{}]", found);
        return found;
    }

    @Override
    public void removeCountdownFromSpace(final Countdown countdown) {
        notNull(countdown, "countdown may not be null");

        final Countdown found = spaceFor(countdown.getId()).takeById(Countdown.class, countdown.getId());
        if (LOG.isDebugEnabled()) {
            final String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(found.getCountdown());
            LOG.debug("Removing countdown with ID {} and time {}", found.getId(), dateTime);
        }
    }

    @Override
    public void publishIntoSpace(final Countdown countdown) {
        notNull(countdown, "countdown may not be null");
        try {
            spaceFor(countdown.getId()).write(countdown);

            LOG.debug("Published Countdown [{}] into space.", countdown);

        } catch (Throwable e) {
            LOG.error("Failed to publish Countdown [{}] into space.", countdown, e);
        }
    }

    private GigaSpace spaceFor(final String countdownId) {
        if (routing.isRoutedToCurrentPartition(countdownId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

}
