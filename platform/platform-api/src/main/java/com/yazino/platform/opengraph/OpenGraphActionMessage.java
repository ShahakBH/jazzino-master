package com.yazino.platform.opengraph;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenGraphActionMessage implements Message<String> {

    private static final long serialVersionUID = 1L;

    @JsonProperty("playerId")
    private BigInteger playerId;

    @JsonProperty("gameType")
    private String gameType;

    @JsonProperty("action")
    private OpenGraphAction action;

    public OpenGraphActionMessage() {
    }

    public OpenGraphActionMessage(final BigInteger playerId,
                                  final String gameType,
                                  final OpenGraphAction action) {
        this.playerId = playerId;
        this.gameType = gameType;
        this.action = action;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public String getMessageType() { // TODO review
        return "not_used";
    }

    public int getRoutingKey() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(gameType)
                .toHashCode();
    }

    public OpenGraphAction getAction() {
        return action;
    }

    public BigInteger getPlayerId() {
        return playerId;
    }

    public String getGameType() {
        return gameType;
    }

    // TODO Warning - equals/hashCode should include the action property but this would interfere with
    // routing.  The routingKey should be independent of hashCode.  When this happens, please fix this.

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
        final OpenGraphActionMessage rhs = (OpenGraphActionMessage) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(gameType, rhs.gameType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return getRoutingKey();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
