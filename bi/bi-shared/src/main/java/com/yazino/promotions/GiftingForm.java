package com.yazino.promotions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import strata.server.lobby.api.promotion.PromotionType;

public class GiftingForm extends PromotionForm {
    private Long reward;
    private String title;
    private String description;
    private String gameType;

    public GiftingForm() {
        setPromoType(PromotionType.GIFTING);
    }

    public void setReward(final Long reward) {
        this.reward = reward;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    @Override
    public String getPromoKey() {
        return "Gifting.";
    }

    public Long getReward() {
        return reward;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getGameType() {
        return gameType;
    }

    @Override
    public Integer getMaxRewards() {
        return 1;//always!
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
        GiftingForm rhs = (GiftingForm) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(this.reward, rhs.reward)
                .append(this.title, rhs.title)
                .append(this.description, rhs.description)
                .append(this.gameType, rhs.gameType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(reward)
                .append(title)
                .append(description)
                .append(gameType)
                .toHashCode();
    }
}
