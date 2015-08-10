package com.yazino.platform.messaging.destination;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class DestinationFactory {
    private final ObserversDestination observersDestination = new ObserversDestination();

    public Destination player(final BigDecimal playerId) {
        return new PlayerDestination(playerId);
    }

    public Destination players(final Set<BigDecimal> playerIds) {
        return new PlayersDestination(playerIds);
    }

    public Destination observers() {
        return observersDestination;
    }

}
