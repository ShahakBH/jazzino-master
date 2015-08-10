package com.yazino.platform.model.chat;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.chat.ChatRequest;
import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SpaceClass
public class GigaspaceChatRequest implements Serializable {

    private static final long serialVersionUID = -2695142627449563340L;

    private String spaceId;
    private ChatRequestType requestType;
    private BigDecimal playerId;
    private String channelId;
    private Map<ChatRequestArgument, String> args;
    private String locationId;

    /**
     * This is a serialisation constructor and should not be called directly.
     */
    public GigaspaceChatRequest() {
        //for Gigaspaces
    }

    /**
     * Construct a template for querying chat request by type.
     *
     * @param requestType the type of request.
     */
    public GigaspaceChatRequest(final ChatRequestType requestType) {
        this.requestType = requestType;
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
    public GigaspaceChatRequest(final ChatRequestType requestType,
                                final BigDecimal playerId,
                                final String channelId,
                                final String locationId,
                                final Map<ChatRequestArgument, String> args) {
        this.requestType = requestType;
        this.playerId = playerId;
        this.channelId = channelId;
        this.locationId = locationId;
        this.args = args;
    }

    /**
     * Create a new chat request without arguments.
     *
     * @param requestType the type of request.
     * @param playerId    the ID of the player.
     * @param channelId   the ID of the channel.
     * @param locationId  the ID of the location.
     */
    public GigaspaceChatRequest(final ChatRequestType requestType,
                                final BigDecimal playerId,
                                final String channelId,
                                final String locationId) {
        this(requestType, playerId, channelId, locationId, new HashMap<ChatRequestArgument, String>());
    }

    public GigaspaceChatRequest(final ChatRequest chatRequest) {
        this(chatRequest.getRequestType(), chatRequest.getPlayerId(), chatRequest.getChannelId(), chatRequest.getLocationId(), null);

        if (chatRequest.getArgs() != null && !chatRequest.getArgs().isEmpty()) {
            this.args = chatRequest.getArgs();
        }
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public ChatRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(final ChatRequestType requestType) {
        this.requestType = requestType;
    }

    @SpaceIndex
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    @SpaceRouting
    @SpaceIndex
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    public Map<ChatRequestArgument, String> getArgs() {
        return args;
    }

    public void setArgs(final Map<ChatRequestArgument, String> args) {
        this.args = args;
    }

    public String getArg(final ChatRequestArgument argument) {
        if (this.args == null || argument == null || !this.args.containsKey(argument)) {
            return null;
        }
        return this.args.get(argument);
    }

    @SpaceIndex
    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(final String locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getAddedPlayerId() {
        if (!args.containsKey(ChatRequestArgument.PLAYER_ID)) {
            return null;
        }
        return new BigDecimal(args.get(ChatRequestArgument.PLAYER_ID));
    }

    public boolean isValid() {
        if (this.requestType == null) {
            return false;
        }
        if (channelId == null || channelId.trim().length() == 0) {
            return false;
        }
        if (playerId == null && (locationId == null || locationId.trim().length() == 0)) {
            return false;
        }
        switch (this.requestType) {
            case ADD_PARTICIPANT:
                if (!args.containsKey(ChatRequestArgument.PLAYER_ID)) {
                    return false;
                }
                if (!args.containsKey(ChatRequestArgument.NICKNAME)) {
                    return false;
                }
                break;
            case LEAVE_CHANNEL:
            case PUBLISH_CHANNEL:
                if (playerId == null) {
                    return false;
                }
                break;
            case SEND_MESSAGE:
                if (playerId == null) {
                    return false;
                }
                if (!args.containsKey(ChatRequestArgument.MESSAGE)) {
                    return false;
                }
                if (args.get(ChatRequestArgument.MESSAGE) == null) {
                    return false;
                }
                if (args.get(ChatRequestArgument.MESSAGE).trim().length() == 0) {
                    return false;
                }
                break;
            case PUBLISH_CHANNELS:
            case LEAVE_ALL:
                if (playerId == null) {
                    return false;
                }
            default:
                return false;
        }
        return true;
    }

    public static GigaspaceChatRequest parse(final String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Invalid message: no arguments");
        }

        String channelId;
        if (args.length > 1) {
            channelId = args[1];
        } else {
            channelId = null;
        }
        if (channelId != null && channelId.trim().length() == 0) {
            channelId = null;
        }
        final String playerIdOrMessage;
        if (args.length > 2) {
            playerIdOrMessage = args[2];
        } else {
            playerIdOrMessage = null;
        }

        final ChatRequestType type = ChatRequestType.findById(args[0]);
        final GigaspaceChatRequest request = new GigaspaceChatRequest(type, null, channelId, null);
        if (ChatRequestType.SEND_MESSAGE.equals(type)) {
            request.getArgs().put(ChatRequestArgument.MESSAGE, playerIdOrMessage);
        } else {
            request.getArgs().put(ChatRequestArgument.PLAYER_ID, playerIdOrMessage);
        }
        return request;
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

        final GigaspaceChatRequest rhs = (GigaspaceChatRequest) obj;
        return new EqualsBuilder()
                .append(requestType, rhs.requestType)
                .append(channelId, rhs.channelId)
                .append(args, rhs.args)
                .append(spaceId, rhs.spaceId)
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
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(requestType)
                .append(playerId)
                .append(channelId)
                .append(args)
                .append(spaceId)
                .toString();
    }
}
