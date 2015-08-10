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
public class GiftPersistenceRequest implements Serializable {
    private static final long serialVersionUID = -8148230809841055161L;

    private String spaceId;
    private BigDecimal recipientPlayerId;
    private BigDecimal giftId;

    public GiftPersistenceRequest() {
        // for GS templates
    }

    public GiftPersistenceRequest(final BigDecimal recipientPlayerId, final BigDecimal giftId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");

        this.recipientPlayerId = recipientPlayerId;
        this.giftId = giftId;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
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
        GiftPersistenceRequest rhs = (GiftPersistenceRequest) obj;
        return new EqualsBuilder()
                .append(this.spaceId, rhs.spaceId)
                .append(this.recipientPlayerId, rhs.recipientPlayerId)
                .append(this.giftId, rhs.giftId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(recipientPlayerId)
                .append(giftId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("spaceId", spaceId)
                .append("recipientPlayerId", recipientPlayerId)
                .append("giftId", giftId)
                .toString();
    }
}
