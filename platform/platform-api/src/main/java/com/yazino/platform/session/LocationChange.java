package com.yazino.platform.session;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class LocationChange implements Serializable {
    private static final long serialVersionUID = 3802773584809295525L;

    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final LocationChangeType type;
    private final Location location;

    public LocationChange(final BigDecimal playerId,
                          final BigDecimal sessionId,
                          final LocationChangeType type,
                          final Location location) {
        notNull(playerId, "playerId may not be null");
        notNull(type, "type may not be null");

        this.playerId = playerId;
        this.sessionId = sessionId;
        this.type = type;
        this.location = location;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public LocationChangeType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
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
        final LocationChange rhs = (LocationChange) obj;
        return new EqualsBuilder()
                .append(type, rhs.type)
                .append(location, rhs.location)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .append(type)
                .append(location)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(sessionId)
                .append(type)
                .append(location)
                .toString();
    }
}
