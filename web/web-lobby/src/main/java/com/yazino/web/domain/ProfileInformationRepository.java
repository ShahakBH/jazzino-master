package com.yazino.web.domain;

import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.KeyGenerator;
import com.googlecode.ehcache.annotations.Property;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.community.ProfileInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service("profileInformationRepository")
public class ProfileInformationRepository {

    private final PlayerService playerService;

    //cglib
    public ProfileInformationRepository() {
        playerService = null;
    }

    @Autowired
    public ProfileInformationRepository(final PlayerService playerService) {
        notNull(playerService, "playerService is null");
        this.playerService = playerService;
    }

    @Cacheable(cacheName = "profileInformationCache",
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")))
    public ProfileInformation getProfileInformation(final BigDecimal playerId,
                                                    final String gameType) {
        return playerService.getProfileInformation(playerId, gameType);
    }
}
