package com.yazino.platform.session;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class PlayerSessionStatus implements Serializable {
    private static final long serialVersionUID = 7602418448498786238L;
    private final BigDecimal playerId;
    private final Set<String> locations;

    public PlayerSessionStatus(final BigDecimal playerId) {
        this.playerId = playerId;
        locations = new HashSet<String>();
    }

    public PlayerSessionStatus(final Set<String> locations,
                               final BigDecimal playerId) {
        this.locations = locations;
        this.playerId = playerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Set<String> getLocations() {
        return locations;
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
        final PlayerSessionStatus rhs = (PlayerSessionStatus) obj;
        return new EqualsBuilder()
                .append(locations, rhs.locations)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(locations)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(locations)
                .toString();
    }


}
