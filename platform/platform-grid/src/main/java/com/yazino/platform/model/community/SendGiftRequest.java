package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class SendGiftRequest implements Serializable {
    private static final long serialVersionUID = -8148230809841052361L;

    private String spaceId;
    private BigDecimal sendingPlayerId;
    private BigDecimal recipientPlayerId;
    private BigDecimal giftId;
    private BigDecimal sessionId;

    public SendGiftRequest() {
        // for GS templates
    }

    public SendGiftRequest(final BigDecimal sendingPlayerId,
                           final BigDecimal recipientPlayerId,
                           final BigDecimal giftId,
                           final BigDecimal sessionId) {
        notNull(sendingPlayerId, "sendingPlayerId may not be null");
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");
        notNull(sessionId, "sessionId may not be null");

        this.sendingPlayerId = sendingPlayerId;
        this.recipientPlayerId = recipientPlayerId;
        this.giftId = giftId;
        this.sessionId = sessionId;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public BigDecimal getSendingPlayerId() {
        return sendingPlayerId;
    }

    public void setSendingPlayerId(final BigDecimal sendingPlayerId) {
        this.sendingPlayerId = sendingPlayerId;
    }

    @SpaceRouting
    public BigDecimal getRecipientPlayerId() {
        return recipientPlayerId;
    }

    public void setRecipientPlayerId(final BigDecimal recipientPlayerId) {
        this.recipientPlayerId = recipientPlayerId;
    }

    public BigDecimal getGiftId() {
        return giftId;
    }

    public void setGiftId(final BigDecimal giftId) {
        this.giftId = giftId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
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
        SendGiftRequest rhs = (SendGiftRequest) obj;
        return new EqualsBuilder()
                .append(this.spaceId, rhs.spaceId)
                .append(this.sendingPlayerId, rhs.sendingPlayerId)
                .append(this.recipientPlayerId, rhs.recipientPlayerId)
                .append(this.giftId, rhs.giftId)
                .append(this.sessionId, rhs.sessionId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(sendingPlayerId)
                .append(recipientPlayerId)
                .append(giftId)
                .append(sessionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("spaceId", spaceId)
                .append("sendingPlayerId", sendingPlayerId)
                .append("recipientPlayerId", recipientPlayerId)
                .append("giftId", giftId)
                .append("sessionId", sessionId)
                .toString();
    }
}
