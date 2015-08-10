package com.yazino.promotions.filter;

import com.google.common.base.Predicate;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;

import static strata.server.lobby.api.promotion.BuyChipsPromotion.CHIP_AMOUNT_IDENTIFIER;

public class NoChipPackageFilter implements Predicate<Promotion> {

    /*
     Returns true if it finds a PromotionConfiguration item identifying a BuyChips Promotion
     */
    @Override
    public boolean apply(final Promotion promotion) {
        final PromotionConfiguration configuration = promotion.getConfiguration();
        if (configuration != null && configuration.hasConfigItems()) {
            for (String key : configuration.getConfiguration().keySet()) {
                if (key.contains(CHIP_AMOUNT_IDENTIFIER)
                        && configuration.getConfigurationValueAsBigDecimal(key) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
