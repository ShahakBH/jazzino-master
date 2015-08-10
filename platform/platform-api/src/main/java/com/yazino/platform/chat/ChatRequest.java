package com.yazino.platform.chat;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatRequest implements Serializable {
    private static final long serialVersionUID = -776928442480229367L;

    private final Map<ChatRequestArgument, String> args = new HashMap<ChatRequestArgument, String>();

    private final ChatRequestType requestType;
    private final BigDecimal playerId;
    private final String channelId;
    private final String locationId;

    /**
     * Create a new chat request without arguments.
     *
     * @param requestType the type of request.
     * @param playerId    the ID of the player.
     * @param channelId   the ID of the channel.
     * @param locationId  the ID of the location.
     */
    public ChatRequest(final ChatRequestType requestType,
                       final BigDecimal playerId,
                       final String channelId,
                       final String locationId) {
        this(requestType, playerId, channelId, locationId, new HashMap<ChatRequestArgument, String>());
    }

    /**
     * Create a new chat request with arguments.
     *
     * @param requestType the type of request.
     * @param playerId    the ID of the player.
     * @param channelId   the ID of the channel.
     * @param locationId  the ID of the location.
     * @param args        any aruments to the request.
     */
    public ChatRequest(final ChatRequestType requestType,
                       final BigDecimal playerId,
                       final String channelId,
                       final String locationId,
                       final Map<ChatRequestArgument, String> args) {
        this.requestType = requestType;
        this.playerId = playerId;
        this.channelId = channelId;
        this.locationId = locationId;

        if (args != null) {
            this.args.putAll(args);
        }
    }

    public ChatRequestType getRequestType() {
        return requestType;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getChannelId() {
        return channelId;
    }

    public Map<ChatRequestArgument, String> getArgs() {
        return Collections.unmodifiableMap(args);
    }

    public String getLocationId() {
        return locationId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final ChatRequest rhs = (ChatRequest) obj;
        return new EqualsBuilder()
                .append(requestType, rhs.requestType)
                .append(channelId, rhs.channelId)
                .append(args, rhs.args)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(requestType)
                .append(BigDecimals.strip(playerId))
                .append(channelId)
                .append(args)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(requestType)
                .append(playerId)
                .append(channelId)
                .append(args)
                .toString();
    }
}
