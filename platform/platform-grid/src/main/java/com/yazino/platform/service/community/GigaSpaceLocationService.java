package com.yazino.platform.service.community;

import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaSpaceLocationService implements LocationService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaSpaceLocationService.class);

    private final GigaSpace globalGigaSpace;

    @Autowired
    public GigaSpaceLocationService(@Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.globalGigaSpace = globalGigaSpace;
    }

    @Override
    public void notify(final BigDecimal playerId,
                       final BigDecimal sessionId,
                       final LocationChangeType locationChangeType,
                       final Location location) {
        notNull(playerId, "playerId may not be null");
        notNull(locationChangeType, "locationChangeType may not be null");
        notNull(location, "location may not be null");

        LOG.debug("Notifying location change for player {} with type {} and location {}", playerId, locationChangeType, location);

        globalGigaSpace.write(new LocationChangeNotification(playerId, sessionId, locationChangeType, location));
    }
}
