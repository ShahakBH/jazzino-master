package com.yazino.platform.tracking;

import com.yazino.platform.Platform;
import com.yazino.platform.event.message.EventMessageType;
import com.yazino.platform.event.message.PlatformEvent;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingEvent implements PlatformEvent, Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("platform")
    private Platform platform;

    @JsonProperty("player_id")
    private BigDecimal playerId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("properties")
    private Map<String, String> eventProperties;

    @JsonProperty
    private DateTime received;

    private TrackingEvent() {
    }

    public TrackingEvent(Platform platform, BigDecimal playerId, String name, Map<String, String> eventProperties, DateTime received) {
        // Platform may be null to support legacy services
        checkNotNull(playerId);
        checkNotNull(name);
        checkArgument(eventProperties == null || eventProperties instanceof Serializable);
        checkNotNull(received);
        this.platform = platform;
        this.playerId = playerId;
        this.name = name;
        this.eventProperties = eventProperties;
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

    public Map<String, String> getEventProperties() {
        return eventProperties;
    }

    public void setEventProperties(Map<String, String> eventProperties) {
        this.eventProperties = eventProperties;
    }

    public DateTime getReceived() {
        return received;
    }

    public void setReceived(DateTime received) {
        this.received = received;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.TRACKING;
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
                .append(name, rhs.name)
                .append(eventProperties, rhs.eventProperties)
                .append(received, rhs.received)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(platform)
                .append(BigDecimals.strip(playerId))
                .append(name)
                .append(eventProperties)
                .append(received)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "TrackingEvent{"
                + "platform='" + platform + '\''
                + ", playerId='" + playerId + '\''
                + ", name='" + name + '\''
                + ", eventProperties='" + eventProperties + '\''
                + ", received='" + received + '\''
                + '}';
    }
}
