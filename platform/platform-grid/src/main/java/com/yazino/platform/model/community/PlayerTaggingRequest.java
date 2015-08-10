package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class PlayerTaggingRequest {
    public enum Action {
        ADD,
        REMOVE
    }

    private Action action;
    private String spaceId;
    private BigDecimal playerId;
    private String tag;

    //for gs
    public PlayerTaggingRequest() {
    }

    public PlayerTaggingRequest(final BigDecimal playerId,
                                final String tag,
                                final Action action) {
        this.playerId = playerId;
        this.tag = tag;
        this.action = action;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PlayerTaggingRequest rhs = (PlayerTaggingRequest) obj;
        return new EqualsBuilder()
                .append(this.spaceId, rhs.spaceId)
                .append(this.playerId, rhs.playerId)
                .append(this.tag, rhs.tag)
                .append(this.action, rhs.action)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(playerId)
                .append(tag)
                .append(action)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(playerId)
                .append(tag)
                .append(action)
                .toString();
    }
}
