package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.gifting.CollectChoice;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class CollectGiftRequest implements Serializable {
    private static final long serialVersionUID = -8148230809841052361L;

    private BigDecimal recipientPlayerId;
    private BigDecimal giftId;
    private BigDecimal sessionId;
    private BigDecimal winnings;
    private CollectChoice choice;

    public CollectGiftRequest() {
        // for GS templates
    }

    public CollectGiftRequest(final BigDecimal recipientPlayerId,
                              final BigDecimal giftId,
                              final BigDecimal sessionId,
                              final BigDecimal winnings,
                              final CollectChoice choice) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");
        notNull(sessionId, "sessionId may not be null");
        notNull(winnings, "winnings may not be null");
        notNull(choice, "choice may not be null");

        this.recipientPlayerId = recipientPlayerId;
        this.giftId = giftId;
        this.sessionId = sessionId;
        this.winnings = winnings;
        this.choice = choice;
    }

    @SpaceRouting
    public BigDecimal getRecipientPlayerId() {
        return recipientPlayerId;
    }

    public void setRecipientPlayerId(final BigDecimal recipientPlayerId) {
        this.recipientPlayerId = recipientPlayerId;
    }

    @SpaceId
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

    public BigDecimal getWinnings() {
        return winnings;
    }

    public void setWinnings(final BigDecimal winnings) {
        this.winnings = winnings;
    }

    public CollectChoice getChoice() {
        return choice;
    }

    public void setChoice(final CollectChoice choice) {
        this.choice = choice;
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
        CollectGiftRequest rhs = (CollectGiftRequest) obj;
        return new EqualsBuilder()
                .append(this.recipientPlayerId, rhs.recipientPlayerId)
                .append(this.giftId, rhs.giftId)
                .append(this.sessionId, rhs.sessionId)
                .append(this.winnings, rhs.winnings)
                .append(this.choice, rhs.choice)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(recipientPlayerId)
                .append(giftId)
                .append(sessionId)
                .append(winnings)
                .append(choice)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("recipientPlayerId", recipientPlayerId)
                .append("giftId", giftId)
                .append("sessionId", sessionId)
                .append("winnings", winnings)
                .append("choice", choice)
                .toString();
    }
}
