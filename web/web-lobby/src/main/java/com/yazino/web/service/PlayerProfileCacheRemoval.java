package com.yazino.web.service;

import com.googlecode.ehcache.annotations.KeyGenerator;
import com.googlecode.ehcache.annotations.Property;
import com.googlecode.ehcache.annotations.TriggersRemove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * TODO: this should be merged into {@link com.yazino.web.domain.ProfileInformationRepository}
 */
@Service("playerProfileCacheRemoval")
public class PlayerProfileCacheRemoval {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerProfileCacheRemoval.class);

    @TriggersRemove(cacheName = "profileInformationCache",
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")))
    public void remove(final BigDecimal playerId,
                       final String gameType) {
        LOG.info(String.format("removing player Id:%s from cache for game:%s", playerId, gameType));
    }
}
