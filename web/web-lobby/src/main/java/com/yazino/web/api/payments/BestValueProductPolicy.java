package com.yazino.web.api.payments;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BestValueProductPolicy {
    /**
     * Calculates which product offers the best value, that is, the most chips per pound.
     *
     * @param products
     * @return the product id of teh product offering best value.
     */
    public String findProductIdOfBestValueProduct(List<ChipProduct> products) {
        if (products == null) {
            return null;
        }
        String bestValueProductId = null;
        BigDecimal maxChipsPerUnitOfCurrency = BigDecimal.ZERO;
        for (ChipProduct product : products) {
            BigDecimal chipsPerUnitOfCurrency;
            if (product.getPromoChips() != null) {
                chipsPerUnitOfCurrency = product.getPromoChips().divide(product.getPrice(), 2);
            } else {
                chipsPerUnitOfCurrency = product.getChips().divide(product.getPrice(), 2);
            }
            if (chipsPerUnitOfCurrency.compareTo(maxChipsPerUnitOfCurrency) == 1) {
                maxChipsPerUnitOfCurrency = chipsPerUnitOfCurrency;
                bestValueProductId = product.getProductId();
            }
        }
        return bestValueProductId;
    }
}
