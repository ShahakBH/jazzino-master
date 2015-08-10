package com.yazino.platform.session;

import com.yazino.platform.table.TableType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static com.yazino.platform.table.TableType.PRIVATE;
import static com.yazino.platform.table.TableType.TOURNAMENT;

public final class Location implements Serializable {
    private static final long serialVersionUID = -7485788782627372152L;

    private final String locationId;
    private final String locationName;
    private final String gameType;
    private final BigDecimal ownerId;
    private final TableType tableType;


    public Location(final String locationId,
                    final String locationName,
                    final String gameType,
                    final BigDecimal ownerId,
                    TableType tableType) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.gameType = gameType;
        this.ownerId = ownerId;
        this.tableType = tableType;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getGameType() {
        return gameType;
    }

    public boolean isPrivateLocation() {
        return PRIVATE == tableType;
    }

    public boolean isTournamentLocation() {
        return TOURNAMENT == tableType;
    }

    public BigDecimal getOwnerId() {
        return ownerId;
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
        final Location rhs = (Location) obj;
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
        return locationName + "(" + locationId + ")";
    }
}
