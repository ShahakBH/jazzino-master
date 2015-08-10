package com.yazino.platform.model.chat;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class ChatParticipant implements Serializable {
    private static final long serialVersionUID = -5737941118662523701L;
    private String spaceId;
    private BigDecimal playerId;
    private String channelId;
    private String nickname;

    public ChatParticipant() {
        //for gigaspace
    }

    public ChatParticipant(final BigDecimal playerId,
                           final String channelId,
                           final String nickname) {
        notNull(playerId, "playerId is null");
        notBlank(channelId, "channelId is blank");
        this.playerId = playerId;
        this.channelId = channelId;
        this.nickname = nickname;
        generateSpaceId();
    }


    public ChatParticipant(final BigDecimal playerId,
                           final String channelId) {
        notNull(playerId, "playerId is null");
        notBlank(channelId, "channelId is blank");
        //for templates
        this.playerId = playerId;
        this.channelId = channelId;
        generateSpaceId();
    }

    public ChatParticipant(final String channelId) {
        this.channelId = channelId;
        //for template
    }

    public ChatParticipant(final BigDecimal playerId) {
        this.playerId = playerId;
        //for template
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    @SpaceId
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
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

    public boolean isNew() {
        return (this.spaceId == null || this.spaceId.trim().length() == 0);
    }

    private void generateSpaceId() {
        this.spaceId = playerId + "|" + channelId;
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
        final ChatParticipant rhs = (ChatParticipant) obj;
        return new EqualsBuilder()
                .append(channelId, rhs.channelId)
                .append(nickname, rhs.nickname)
                .append(spaceId, rhs.spaceId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(channelId)
                .append(nickname)
                .append(BigDecimals.strip(playerId))
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(channelId)
                .append(nickname)
                .append(playerId)
                .append(spaceId)
                .toString();
    }
}
