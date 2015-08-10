package com.yazino.platform.event.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusCollectedEvent implements PlatformEvent {

    private static final long serialVersionUID = 1755981796182147814L;
    @JsonProperty("id")
    private BigDecimal playerId;

    @JsonProperty("cTs")
    private DateTime collected;

    protected BonusCollectedEvent() {
    }

    public BonusCollectedEvent(final BigDecimal playerId, final DateTime collected) {
        notNull(playerId, "playerId may not be null");
        notNull(collected, "balance may not be null");
        this.playerId = playerId;
        this.collected = collected;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public DateTime getCollected() {
        return collected;
    }

    private void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    private void setCollected(final DateTime collected) {
        this.collected = collected;
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
        final BonusCollectedEvent rhs = (BonusCollectedEvent) obj;
        return new EqualsBuilder()
                .append(collected, rhs.collected)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(collected)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "BonusCollectedEvent{"
                + "playerId='" + playerId + '\''
                + ", collected='" + collected.toString() + '\''
                + '}';
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.BONUS;
    }
}
