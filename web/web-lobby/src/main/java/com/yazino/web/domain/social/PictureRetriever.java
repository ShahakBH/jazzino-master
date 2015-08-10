package com.yazino.web.domain.social;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.web.data.BasicProfileInformationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class PictureRetriever implements PlayerInformationRetriever {
    private final BasicProfileInformationRepository repository;

    @Autowired
    public PictureRetriever(final BasicProfileInformationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.PICTURE;
    }

    @Override
    public String retrieveInformation(final BigDecimal playerId,
                                      final String gameType) {
        final BasicProfileInformation profile = repository.getBasicProfileInformation(playerId);
        if (profile == null) {
            return null;
        }
        return profile.getPictureUrl();
    }
}
