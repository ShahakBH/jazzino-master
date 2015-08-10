package com.yazino.promotions;

import strata.server.lobby.api.promotion.Promotion;

public interface PromotionBuilder<T extends Promotion> {

    T build();

}
