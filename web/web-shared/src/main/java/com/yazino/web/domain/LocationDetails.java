package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class LocationDetails implements Serializable {
    private static final long serialVersionUID = 985457887966061997L;
    private final BigDecimal locationId;
    private final String locationName;
    private final String gameType;

    public LocationDetails(final BigDecimal locationId,
                           final String locationName,
                           final String gameType) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.gameType = gameType;
    }

    public BigDecimal getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getGameType() {
        return gameType;
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
        final LocationDetails rhs = (LocationDetails) obj;
        return new EqualsBuilder()
                .append(locationId, rhs.locationId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(locationId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(locationId)
                .append(locationName)
                .append(gameType)
                .toString();
    }
}
