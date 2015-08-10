package com.yazino.bi.operations.view;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.DailyAwardPromotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.MAX_REWARDS_KEY;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.REWARD_CHIPS_KEY;

public class DailyAwardPromotionForm extends PromotionForm<DailyAwardPromotion> {
    private Integer rewardChips;

    public DailyAwardPromotionForm() {
        setPromotionType(PromotionType.DAILY_AWARD);
    }

    public DailyAwardPromotionForm(final DailyAwardPromotion promotion) {
        super(promotion);

        final PromotionConfiguration config = promotion.getConfiguration();
        if (config != null && config.hasConfigItems()) {
            rewardChips = config.getConfigurationValueAsInteger(REWARD_CHIPS_KEY);
        }
    }

    @Override
    public DailyAwardPromotion buildPromotion() {
        DailyAwardPromotion promotion = super.buildPromotion();
        promotion.addConfigurationItem(REWARD_CHIPS_KEY, getRewardChips().toString());
        promotion.addConfigurationItem(MAX_REWARDS_KEY, getMaximumRewards().toString());
        return promotion;
    }

    public Integer getRewardChips() {
        return rewardChips;
    }

    public void setRewardChips(final Integer rewardChips) {
        this.rewardChips = rewardChips;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
