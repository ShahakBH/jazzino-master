package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class PlayerPersistenceRequest {
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String status = STATUS_PENDING;
    private BigDecimal playerId;
    private String spaceId;

    public PlayerPersistenceRequest() {
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public PlayerPersistenceRequest(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    @SpaceIndex
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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
        final PlayerPersistenceRequest rhs = (PlayerPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(status, rhs.status)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(BigDecimals.strip(playerId))
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(playerId)
                .append(status)
                .toString();
    }
}
