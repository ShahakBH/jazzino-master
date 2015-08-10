package com.yazino.bi.opengraph;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenGraphCredentialsMessage implements Message<String> {

    private static final long serialVersionUID = 1L;

    @JsonProperty("playerId")
    private BigInteger playerId;

    @JsonProperty("gameType")
    private String gameType;

    @JsonProperty("accessToken")
    private String accessToken;

    public OpenGraphCredentialsMessage() {
    }

    public OpenGraphCredentialsMessage(final BigInteger playerId,
                                       final String gameType,
                                       final String accessToken) {
        this.playerId = playerId;
        this.gameType = gameType;
        this.accessToken = accessToken;
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

    public BigInteger getPlayerId() {
        return playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public String getAccessToken() {
        return accessToken;
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
        final OpenGraphCredentialsMessage rhs = (OpenGraphCredentialsMessage) obj;
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
