package com.yazino.promotions;

public interface PromotionFormDefinitionDao<T> {

    Long save(T promotionForm);

    void update(T promotionForm);

    T getForm(Long campaignId);
}
