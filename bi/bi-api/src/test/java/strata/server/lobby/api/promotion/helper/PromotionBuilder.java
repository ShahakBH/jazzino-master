package strata.server.lobby.api.promotion.helper;

import strata.server.lobby.api.promotion.Promotion;

public interface PromotionBuilder<T extends Promotion> {

    T getPromotion();

}
