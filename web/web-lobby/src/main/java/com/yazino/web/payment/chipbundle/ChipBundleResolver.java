package com.yazino.web.payment.chipbundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Products that can be sold on Android stores, i.e. Google and Amazon.
 */
@Component
public class ChipBundleResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ChipBundleResolver.class);

    // Iteration order is important, as we want to display products in known order, hence use of LinkedHashMap.
    @Resource(name = "chipBundles")
    private Map<String, LinkedHashMap<String, ChipBundle>> chipBundles;

    public ChipBundleResolver() {
    }

    public void setChipBundles(final Map<String, LinkedHashMap<String, ChipBundle>> chipBundles) {
        this.chipBundles = chipBundles;
    }


    public ChipBundle findChipBundleFor(final String gameType, final String chipValueId) {
        if (StringUtils.isBlank(gameType) || StringUtils.isBlank(chipValueId)) {
            return null;
        }

        final Map<String, ChipBundle> chipBundlesForGame = chipBundles.get(gameType);
        if (chipBundlesForGame != null) {
            final ChipBundle bundle = chipBundlesForGame.get(chipValueId);
            LOG.debug("Product {} matched to {}", chipValueId, bundle);
            return bundle;
        }
        LOG.warn("No match found for gameType {} and chipValueId {}", gameType, chipValueId);

        return null;
    }

    public ChipBundle findChipBundleForProductId(final String gameType, final String productId) {
        LOG.debug("Finding chips for product {}, gameType {}", productId, gameType);

        if (StringUtils.isBlank(gameType) || StringUtils.isBlank(productId)) {
            LOG.debug("Cannot find chip amount for gameType {} and productId {}", gameType, productId);
            return null;
        }

        final Map<String, ChipBundle> chipProductsForGame = chipBundles.get(gameType);
        if (chipProductsForGame != null) {
            for (ChipBundle chipBundle : chipProductsForGame.values()) {
                if (productId.equals(chipBundle.getProductId())) {
                    LOG.debug("Product {} matched to {}", productId, chipBundle);
                    return chipBundle;
                }
            }
        }
        LOG.warn("No match found for gameType {} and product {}", gameType, productId);

        return null;
    }

    public List<String> getProductIdsFor(final String gameType) {
        Validate.notBlank(gameType, "gameType cannot be null or blank");

        final Map<String, ChipBundle> productMap = chipBundles.get(gameType);
        if (productMap == null) {
            return Collections.emptyList();
        }
        List<String> productIds = new ArrayList<String>();
        for (ChipBundle chipBundle : productMap.values()) {
            productIds.add(chipBundle.getProductId());
        }
        return productIds;
    }
}
