package com.yazino.bi.tracking;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Map;

public class TrackingEvent {

    private Platform platform;

    private BigDecimal playerId;

    private String name;

    private Map<String, String> properties;

    private DateTime received;

    private TrackingEvent() {
    }

    public TrackingEvent(Platform platform, BigDecimal playerId, String name, Map<String, String> properties, DateTime received) {
        this.platform = platform;
        this.playerId = playerId;
        this.name = name;
        this.properties = properties;
        this.received = received;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public DateTime getReceived() {
        return received;
    }

    public void setReceived(DateTime received) {
        this.received = received;
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
        final TrackingEvent rhs = (TrackingEvent) obj;
        return new EqualsBuilder()
                .append(platform, rhs.platform)
                .append(playerId, rhs.playerId)
                .append(name, rhs.name)
                .append(properties, rhs.properties)
                .append(received, rhs.received)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(platform)
                .append(playerId)
                .append(name)
                .append(properties)
                .append(received)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "TrackingEvent{"
                + "platform='" + platform + '\''
                + ", playerId='" + playerId + '\''
                + ", name='" + name + '\''
                + ", eventProperties='" + properties + '\''
                + ", received='" + received + '\''
                + '}';
    }
}
