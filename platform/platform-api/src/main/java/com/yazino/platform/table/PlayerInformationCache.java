package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerInformationCache implements Serializable {
    private static final long serialVersionUID = -8254057821418829901L;

    private static final Logger LOG = LoggerFactory.getLogger(PlayerInformationCache.class);

    private final Map<BigDecimal, PlayerInformation> cache = new ConcurrentHashMap<BigDecimal, PlayerInformation>();

    public PlayerInformation get(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        return cache.get(playerId);
    }

    public void add(final PlayerInformation playerInformation) {
        notNull(playerInformation, "Player Information may not be null");

        LOG.debug("Adding player information to cache: {}", playerInformation);

        cachePlayerInformation(playerInformation);
    }

    private void cachePlayerInformation(final PlayerInformation playerInformation) {
        LOG.debug("Caching player [{}]", playerInformation.getPlayerId());

        cache.put(playerInformation.getPlayerId(), playerInformation);
    }

    public void clear() {
        cache.clear();
    }

    public void retainOnly(final Collection<BigDecimal> playerIdsToRetain) {
        if (playerIdsToRetain == null || playerIdsToRetain.isEmpty()) {
            clear();
            return;
        }

        cache.keySet().retainAll(playerIdsToRetain);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PlayerInformationCache rhs = (PlayerInformationCache) obj;
        return new EqualsBuilder()
                .append(cache, rhs.cache)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(cache)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(cache)
                .toString();
    }

    public Collection<PlayerInformation> getAll() {
        return Collections.unmodifiableCollection(this.cache.values());
    }
}
