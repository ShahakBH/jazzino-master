package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("basicProfileInformationRepository")
public class BasicProfileInformationRepository {

    private final PlayerService playerService;

    //cglib
    BasicProfileInformationRepository() {
        playerService = null;
    }

    @Autowired
    public BasicProfileInformationRepository(final PlayerService playerService) {
        notNull(playerService, "playerService may not be null");
        this.playerService = playerService;
    }

    @Cacheable(cacheName = "playerDetailsCache")
    public BasicProfileInformation getBasicProfileInformation(final BigDecimal playerId) {
        return playerService.getBasicProfileInformation(playerId);
    }
}
