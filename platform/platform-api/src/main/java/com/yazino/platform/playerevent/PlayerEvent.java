package com.yazino.platform.playerevent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class PlayerEvent {
    private BigDecimal playerId;
    private PlayerEventType eventType;
    private String[] parameters;

    @JsonCreator
    public PlayerEvent(@JsonProperty("playerId") final BigDecimal playerId,
                       @JsonProperty("eventType") final PlayerEventType eventType,
                       @JsonProperty("parameters") final String... parameters) {
        this.playerId = playerId;
        this.eventType = eventType;
        this.parameters = parameters;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public PlayerEventType getEventType() {
        return eventType;
    }

    public String[] getParameters() {
        return parameters;
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
        final PlayerEvent rhs = (PlayerEvent) obj;
        return new EqualsBuilder()
                .append(eventType, rhs.eventType)
                .append(parameters, rhs.parameters)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(eventType)
                .append(parameters)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(eventType)
                .append(parameters)
                .toString();
    }

}
