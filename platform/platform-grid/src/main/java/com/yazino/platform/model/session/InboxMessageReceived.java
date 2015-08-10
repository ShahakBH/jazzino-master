package com.yazino.platform.model.session;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class InboxMessageReceived implements Serializable {
    private static final long serialVersionUID = -1483297728856539758L;

    private String spaceId;
    private BigDecimal playerId;
    private InboxMessage message;

    public InboxMessageReceived() {
    }

    public InboxMessageReceived(final InboxMessage message) {
        notNull(message, "message may not be null");

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
        final InboxMessageReceived rhs = (InboxMessageReceived) obj;
        return new EqualsBuilder()
                .append(message, rhs.message)
                .append(spaceId, rhs.spaceId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(message)
                .append(BigDecimals.strip(playerId))
                .append(spaceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(message)
                .append(playerId)
                .append(spaceId)
                .toString();
    }
}
