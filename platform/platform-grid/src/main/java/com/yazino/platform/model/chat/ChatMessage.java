package com.yazino.platform.model.chat;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = -5778824291995758485L;
    private final BigDecimal playerId;
    private final String nickname;
    private final String message;
    private final String channelId;
    private final String locationId;

    public ChatMessage(final BigDecimal playerId,
                       final String nickname,
                       final String message,
                       final String channelId,
                       final String locationId) {
        this.playerId = playerId;
        this.nickname = nickname;
        this.message = message;
        this.channelId = channelId;
        this.locationId = locationId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getMessage() {
        return message;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getNickname() {
        return nickname;
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
        final ChatMessage rhs = (ChatMessage) obj;
        return new EqualsBuilder()
                .append(channelId, rhs.channelId)
                .append(locationId, rhs.locationId)
                .append(message, rhs.message)
                .append(nickname, rhs.nickname)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(channelId)
                .append(locationId)
                .append(message)
                .append(nickname)
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(channelId)
                .append(locationId)
                .append(message)
                .append(nickname)
                .append(playerId)
                .toString();
    }
}
