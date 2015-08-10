package com.yazino.promotions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class PromotionPlayerReward {
    private static final long serialVersionUID = -1365037865712354135L;

    private Long promoId;
    private BigDecimal playerId;
    private boolean controlGroup;
    private DateTime rewardDate;
    private String details;

    public PromotionPlayerReward(final Long promoId,
                                 final BigDecimal playerId,
                                 final boolean controlGroup,
                                 final DateTime rewardDate,
                                 final String details) {
        this.promoId = promoId;
        this.playerId = playerId;
        this.controlGroup = controlGroup;
        this.rewardDate = rewardDate;
        this.details = details;
    }

    public Long getPromoId() {
        return promoId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public boolean isControlGroup() {
        return controlGroup;
    }

    public DateTime getRewardDate() {
        return rewardDate;
    }

    public String getDetails() {
        return details;
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
        final PromotionPlayerReward rhs = (PromotionPlayerReward) obj;
        return new EqualsBuilder()
                .append(promoId, rhs.promoId)
                .append(playerId, rhs.playerId)
                .append(controlGroup, rhs.controlGroup)
                .append(rewardDate, rhs.rewardDate)
                .append(details, rhs.details)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(promoId)
                .append(playerId)
                .append(controlGroup)
                .append(rewardDate)
                .append(details)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
