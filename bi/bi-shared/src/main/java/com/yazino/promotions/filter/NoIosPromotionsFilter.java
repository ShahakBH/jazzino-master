package com.yazino.promotions.filter;

import com.google.common.base.Predicate;
import com.yazino.platform.Platform;
import strata.server.lobby.api.promotion.Promotion;

public class NoIosPromotionsFilter implements Predicate<Promotion> {

    @Override
    public boolean apply(final Promotion promotion) {
        return promotion.getPlatforms().contains(Platform.IOS);
    }
}
