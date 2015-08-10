package com.yazino.promotions.filter;

import com.google.common.base.Predicate;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.REWARD_CHIPS_KEY;

public class NoRewardChipsFilter implements Predicate<Promotion> {
    @Override
    public boolean apply(final Promotion promotion) {
        final PromotionConfiguration configuration = promotion.getConfiguration();
        if ((configuration != null)
                && configuration.hasConfigItems()
                && (configuration.getConfigurationValueAsInteger(REWARD_CHIPS_KEY) != null)) {
            return true;
        }
        return false;
    }
}
