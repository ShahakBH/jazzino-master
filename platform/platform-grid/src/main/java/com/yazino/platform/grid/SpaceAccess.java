package com.yazino.platform.grid;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class SpaceAccess {
    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;

    @Autowired
    public SpaceAccess(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                       @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                       final Routing routing) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
    }

    public GigaSpace local() {
        return localGigaSpace;
    }

    public GigaSpace global() {
        return globalGigaSpace;
    }

    public boolean isRoutedLocally(final Object routingObject) {
        notNull(routingObject, "routingObject may not be null");

        return routing.isRoutedToCurrentPartition(routingObject);
    }

    public GigaSpace forRouting(final Object routingObject) {
        notNull(routingObject, "routingObject may not be null");

        if (routing.isRoutedToCurrentPartition(routingObject)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
