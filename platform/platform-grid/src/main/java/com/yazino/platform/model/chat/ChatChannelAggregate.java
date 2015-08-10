package com.yazino.platform.model.chat;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;

public class ChatChannelAggregate {

    public class AggregateParticipant {
        public AggregateParticipant(final ChatParticipant participant) {
            this.nickname = participant.getNickname();
            this.playerId = participant.getPlayerId();
        }

        private String nickname;
        private BigDecimal playerId;

        public String getNickname() {
            return nickname;
        }

        public BigDecimal getPlayerId() {
            return playerId;
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
            final AggregateParticipant rhs = (AggregateParticipant) obj;
            return new EqualsBuilder()
                    .append(nickname, rhs.nickname)
                    .isEquals()
                    && BigDecimals.equalByComparison(playerId, rhs.playerId);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(nickname)
                    .append(BigDecimals.strip(playerId))
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append(nickname)
                    .append(playerId)
                    .toString();
        }
    }

    private static final String ADD_PARTICIPANTS = "AddParticipants";
    private static final String LEAVE = "Leave";
    private static final String SEND_MESSAGE = "SendMessage";

    private String id;
    private ChatChannelType type;
    private String locationId;
    private final boolean canAddParticipant;
    private final AggregateParticipant[] chatParticipants;
    private final String[] allowedActions;

    public ChatChannelAggregate(final ChatChannel chatChannel,
                                final ChatParticipant... participants) {
        if (chatChannel != null && chatChannel.getChannelType() != null) {
            this.canAddParticipant = chatChannel.getChannelType().canAddParticipants();
            this.id = chatChannel.getChannelId();
            this.type = chatChannel.getChannelType();
            this.locationId = chatChannel.getLocationId();
            final ArrayList<String> currentlyAllowedActions = new ArrayList<String>();
            if (chatChannel.getChannelType().canAddParticipants()) {
                currentlyAllowedActions.add(ADD_PARTICIPANTS);
            }
            if (chatChannel.getChannelType().canParticipantsLeave()) {
                currentlyAllowedActions.add(LEAVE);
            }
            currentlyAllowedActions.add(SEND_MESSAGE);
            this.allowedActions = currentlyAllowedActions.toArray(new String[currentlyAllowedActions.size()]);

        } else {
            this.canAddParticipant = false;
            this.allowedActions = new String[0];
        }
        if (participants == null) {
            this.chatParticipants = new AggregateParticipant[0];
        } else {
            this.chatParticipants = new AggregateParticipant[participants.length];
            int index = 0;
            for (ChatParticipant participant : participants) {
                this.chatParticipants[index++] = new AggregateParticipant(participant);
            }
        }
    }

    public AggregateParticipant[] getChatParticipants() {
        return chatParticipants;
    }

    public boolean canAddParticipants() {
        return canAddParticipant;
    }

    public String getId() {
        return id;
    }

    public ChatChannelType getType() {
        return type;
    }

    public String getLocationId() {
        return locationId;
    }

    public boolean isCanAddParticipant() {
        return canAddParticipant;
    }

    public String[] getAllowedActions() {
        return allowedActions;
    }

    public boolean hasParticipant(final BigDecimal playerId) {
        if (playerId == null) {
            return false;
        }
        for (AggregateParticipant participant : chatParticipants) {
            if (playerId.equals(participant.getPlayerId())) {
                return true;
            }
        }
        return false;
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
        final ChatChannelAggregate rhs = (ChatChannelAggregate) obj;
        return new EqualsBuilder()
                .append(canAddParticipant, rhs.canAddParticipant)
                .append(chatParticipants, rhs.chatParticipants)
                .append(id, rhs.id)
                .append(locationId, rhs.locationId)
                .append(type, rhs.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(canAddParticipant)
                .append(chatParticipants)
                .append(id)
                .append(locationId)
                .append(type)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(canAddParticipant)
                .append(chatParticipants)
                .append(id)
                .append(locationId)
                .append(type)
                .toString();
    }
}
