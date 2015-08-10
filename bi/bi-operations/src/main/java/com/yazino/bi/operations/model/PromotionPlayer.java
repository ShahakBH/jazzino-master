package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigInteger;

public class PromotionPlayer {
    private Long promotionId;
    private BigInteger playerId;

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
    }

    public BigInteger getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigInteger playerId) {
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
        final PromotionPlayer rhs = (PromotionPlayer) obj;
        return new EqualsBuilder()
                .append(promotionId, rhs.promotionId)
                .append(playerId, rhs.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(promotionId)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
