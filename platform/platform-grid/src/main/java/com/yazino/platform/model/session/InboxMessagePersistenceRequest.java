package com.yazino.platform.model.session;

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

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class InboxMessagePersistenceRequest implements Serializable {
    private static final long serialVersionUID = 2352589875099564011L;

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ERROR = "Error";

    private String spaceId;
    private BigDecimal playerId;
    private InboxMessage message;

    private String status = STATUS_PENDING;

    public InboxMessagePersistenceRequest() {
    }

    public InboxMessagePersistenceRequest(final InboxMessage message) {
        notNull(message, "message is null");

        this.message = message;
        this.playerId = message.getPlayerId();
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public InboxMessage getMessage() {
        return message;
    }

    public void setMessage(final InboxMessage message) {
        this.message = message;
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
        final InboxMessagePersistenceRequest rhs = (InboxMessagePersistenceRequest) obj;
        return new EqualsBuilder()
                .append(message, rhs.message)
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
                .append(message)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(playerId)
                .append(message)
                .append(status)
                .toString();
    }
}
