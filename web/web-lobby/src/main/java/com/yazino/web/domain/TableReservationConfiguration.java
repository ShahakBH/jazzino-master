package com.yazino.web.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component("tableReservationConfiguration")
public class TableReservationConfiguration {
    private final Set<String> reservationDisabled = new HashSet<String>();

    @Autowired
    public TableReservationConfiguration(@Value("${strata.reservation.excluded-game-types}") final String commaSeparatedGameTypes) {
        if (commaSeparatedGameTypes != null) {
            final String[] gameTypes = commaSeparatedGameTypes.split(",");
            for (String gameType : gameTypes) {
                reservationDisabled.add(gameType.trim());
            }
        }
    }

    public boolean supportsReservation(final String gameType) {
        return !reservationDisabled.contains(gameType);
    }
}
