package com.yazino.web.domain.social;

import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.data.SessionStatusRepository;
import com.yazino.web.domain.LocationDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@Qualifier("playerInformationRetriever")
public class LocationsRetriever implements PlayerInformationRetriever {
    private final SessionStatusRepository sessionStatusRepository;
    private final LocationDetailsRepository locationDetailsRepository;

    @Autowired
    public LocationsRetriever(final SessionStatusRepository sessionStatusRepository,
                              final LocationDetailsRepository locationDetailsRepository) {
        this.sessionStatusRepository = sessionStatusRepository;
        this.locationDetailsRepository = locationDetailsRepository;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.LOCATIONS;
    }

    @Override
    public Object retrieveInformation(final BigDecimal playerId, final String gameType) {
        final Set<LocationDetails> result = new HashSet<>();
        final PlayerSessionStatus status = sessionStatusRepository.getStatus(playerId);
        if (status == null) {
            return null;
        }
        for (String location : status.getLocations()) {
            final LocationDetails details = locationDetailsRepository.getLocationDetails(new BigDecimal(location));
            if (gameType == null || (details != null && details.getGameType().equals(gameType))) {
                result.add(details);
            }
        }
        return result;
    }
}
