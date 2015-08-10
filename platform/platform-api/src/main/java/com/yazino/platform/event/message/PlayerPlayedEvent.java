package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerPlayedEvent implements PlatformEvent {
    private static final long serialVersionUID = 8981836447458558028L;

    @JsonProperty("id")
    private BigDecimal playerId;
    @JsonProperty("dob")
    private DateTime time;

    private PlayerPlayedEvent() {
    }

    public PlayerPlayedEvent(final BigDecimal playerId,
                             final DateTime time) {
        this.playerId = playerId;
        this.time = time;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.PLAYED;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(final DateTime time) {
        this.time = time;
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
        final PlayerPlayedEvent rhs = (PlayerPlayedEvent) obj;
        return new EqualsBuilder()
                .append(time, rhs.time)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(time)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerPlayedEvent{"
                + "playerId=" + playerId
                + ", time=" + time
                + '}';
    }
}
