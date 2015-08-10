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
public class AcknowledgeGiftRequest implements Serializable {
    private static final long serialVersionUID = -8148230809841052361L;

    private BigDecimal recipientPlayerId;
    private BigDecimal giftId;

    public AcknowledgeGiftRequest() {
        // for GS templates
    }

    public AcknowledgeGiftRequest(final BigDecimal recipientPlayerId,
                                  final BigDecimal giftId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");

        this.recipientPlayerId = recipientPlayerId;
        this.giftId = giftId;
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
        AcknowledgeGiftRequest rhs = (AcknowledgeGiftRequest) obj;
        return new EqualsBuilder()
                .append(this.recipientPlayerId, rhs.recipientPlayerId)
                .append(this.giftId, rhs.giftId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(recipientPlayerId)
                .append(giftId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("recipientPlayerId", recipientPlayerId)
                .append("giftId", giftId)
                .toString();
    }
}
