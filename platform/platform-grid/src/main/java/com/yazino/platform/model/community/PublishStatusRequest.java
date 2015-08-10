package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class PublishStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String spaceId;
    private BigDecimal playerId;
    private PublishStatusRequestType requestType;
    private String gameType;

    // GS constructor
    public PublishStatusRequest() {
    }

    public PublishStatusRequest(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public PublishStatusRequest(final String requestType) {
        this.requestType = PublishStatusRequestType.valueOf(requestType);
    }

    public PublishStatusRequest(final BigDecimal playerId,
                                final String gameType) {
        this(playerId, PublishStatusRequestType.COMMUNITY_STATUS, gameType);
    }

    public PublishStatusRequest(final BigDecimal playerId,
                                final PublishStatusRequestType requestType) {
        this(playerId, requestType, null);
    }

    public PublishStatusRequest(final BigDecimal playerId,
                                final PublishStatusRequestType type,
                                final String gameType) {
        this.playerId = playerId;
        this.requestType = type;
        this.gameType = gameType;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public PublishStatusRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(final PublishStatusRequestType requestType) {
        this.requestType = requestType;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
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
        final PublishStatusRequest rhs = (PublishStatusRequest) obj;
        return new EqualsBuilder()
                .append(requestType, rhs.requestType)
                .append(spaceId, rhs.spaceId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(requestType)
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(requestType)
                .append(spaceId)
                .toString();
    }

}
