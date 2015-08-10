package com.yazino.promotions;

import java.math.BigDecimal;
import java.util.Map;

public interface PromotionChipsPercentageDao {


    Map<Integer, BigDecimal> getChipsPercentages(Long promotionDefinitionId);

    void save(Long promotionDefinitionId, Map<Integer, BigDecimal> chipsPercentagePackages);

    void updateChipsPercentage(Long promotionDefinitionId, Map<Integer, BigDecimal> chipsPackagePercentages);
}
