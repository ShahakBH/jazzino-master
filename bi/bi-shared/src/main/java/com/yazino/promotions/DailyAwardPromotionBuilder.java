package com.yazino.promotions;

import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.DailyAwardPromotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.List;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

public class DailyAwardPromotionBuilder implements PromotionBuilder<DailyAwardPromotion> {


    private DailyAwardPromotion promo;

    public DailyAwardPromotionBuilder() {
        promo = new DailyAwardPromotion();
    }

    @Override
    public DailyAwardPromotion build() {
        return promo;
    }

    public DailyAwardPromotionBuilder withReward(Integer rewardChips) {
        if (rewardChips == null) {
            promo.getConfiguration().getConfiguration().remove(REWARD_CHIPS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(REWARD_CHIPS_KEY, rewardChips.toString());
        }
        return this;
    }

    public DailyAwardPromotionBuilder withPlatforms(final List<Platform> platforms) {
        promo.setPlatforms(platforms);
        return this;
    }

    public DailyAwardPromotionBuilder withStartDate(final DateTime startDAte) {
        promo.setStartDate(startDAte);
        return this;
    }

    public DailyAwardPromotionBuilder withEndDate(final DateTime endDAte) {
        promo.setEndDate(endDAte);
        return this;
    }

    public DailyAwardPromotionBuilder withId(final Long id) {
        promo.setId(id);
        return this;
    }

    public DailyAwardPromotion getPromotion() {
        return promo;
    }

    public DailyAwardPromotionBuilder withAllPlayers(boolean allPlayers) {
        promo.setAllPlayers(allPlayers);
        return this;
    }

    public DailyAwardPromotionBuilder withConfiguration(final PromotionConfiguration config) {
        promo.setConfiguration(config);
        return this;
    }

    public DailyAwardPromotionBuilder withName(final String name) {
        promo.setName(name);
        return this;
    }

    public DailyAwardPromotionBuilder withMaxRewards(Integer maxRewards) {
        if (maxRewards == null) {
            promo.getConfiguration().getConfiguration().remove(MAX_REWARDS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(MAX_REWARDS_KEY, maxRewards.toString());
        }
        return this;
    }

    public DailyAwardPromotionBuilder withPriority(Integer priority) {
        promo.setPriority(priority);
        return this;
    }

    public DailyAwardPromotionBuilder withMainImage(final String mainImageValue) {
        promo.getConfiguration().addConfigurationItem(MAIN_IMAGE_KEY, mainImageValue);
        return this;
    }

    public DailyAwardPromotionBuilder withMainImageLink(final String mainImageLink) {
        promo.getConfiguration().addConfigurationItem(MAIN_IMAGE_LINK_KEY, mainImageLink);
        return this;
    }

    public DailyAwardPromotionBuilder withSecImage(final String secImageValue) {
        promo.getConfiguration().addConfigurationItem(SECONDARY_IMAGE_KEY, secImageValue);
        return this;
    }

    public DailyAwardPromotionBuilder withSecImageLink(final String secImageLink) {
        promo.getConfiguration().addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, secImageLink);
        return this;
    }

    public DailyAwardPromotionBuilder withSeed(Integer seed) {
        promo.setSeed(seed);
        return this;
    }

    public DailyAwardPromotionBuilder withControlGroupPercentage(Integer controlGroupPercentage) {
        promo.setControlGroupPercentage(controlGroupPercentage);
        return this;
    }

    public DailyAwardPromotionBuilder withControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        promo.setControlGroupFunction(controlGroupFunction);
        return this;
    }

    public DailyAwardPromotionBuilder withPromotionType(final PromotionType promotionType) {
        promo.setPromotionType(promotionType);
        return this;
    }

}
