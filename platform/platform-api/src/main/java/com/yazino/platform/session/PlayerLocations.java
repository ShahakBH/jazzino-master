package com.yazino.platform.session;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

public class PlayerLocations implements Serializable {
    private static final long serialVersionUID = -6757679891790011906L;

    private final BigDecimal playerId;
    private final Collection<BigDecimal> locationIds;

    public PlayerLocations(final BigDecimal playerId,
                           final Collection<BigDecimal> locationIds) {
        this.playerId = playerId;
        this.locationIds = locationIds;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Collection<BigDecimal> getLocationIds() {
        return locationIds;
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
        final PlayerLocations rhs = (PlayerLocations) obj;
        return new EqualsBuilder()
                .append(locationIds, rhs.locationIds)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(locationIds)
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerLocations{"
                + "playerId=" + playerId
                + ", locationIds=" + locationIds
                + '}';
    }
}
