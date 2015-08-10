package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * notification that a player has joined or left a location. NotificationType enum specifies whether the player
 * has joined or left.
 */
@SpaceClass(replicate = false)
public class LocationChangeNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private String spaceId;
    private BigDecimal playerId;
    private BigDecimal sessionId;
    private LocationChangeType notificationType;
    private Location location;

    public LocationChangeNotification() {
        // CGLib and template constructor
    }

    public LocationChangeNotification(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public LocationChangeNotification(final BigDecimal playerId,
                                      final BigDecimal sessionId,
                                      final LocationChangeType notificationType,
                                      final Location location) {
        notNull(playerId, "playerId may not be null");
        notNull(notificationType, "locationChangeType may not be null");

        this.playerId = playerId;
        this.sessionId = sessionId;
        this.notificationType = notificationType;
        this.location = location;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
    }

    public LocationChangeType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(final LocationChangeType notificationType) {
        this.notificationType = notificationType;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(final Location location) {
        this.location = location;
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
        final LocationChangeNotification rhs = (LocationChangeNotification) obj;
        return new EqualsBuilder()
                .append(location, rhs.location)
                .append(notificationType, rhs.notificationType)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(sessionId, rhs.sessionId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(location)
                .append(notificationType)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(sessionId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
