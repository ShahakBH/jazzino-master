package com.yazino.web.api.payments;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This policy will return the id of the forth most expensive package.
 * Bodge. Currently the most popular product in ChipProducts, is hard coded to be the third cheapest dollar package.
 * So for android the $15 dollar package, for FACEBOOK $20 package
 * This is slightly daft. In future we may wish to set this in another way.
 */
@Service
public class MostPopularProductPolicy {
    // the most popular package is the third cheapest
    private static final int POSITION_OF_MOST_POPULAR_PRODUCT = 3;

    public String findProductIdOfMostPopularProduct(List<ChipProduct> chipProducts) {
        if (chipProducts == null || chipProducts.size() < POSITION_OF_MOST_POPULAR_PRODUCT) {
            return null;
        }
        ArrayList<ChipProduct> products = Lists.newArrayList(chipProducts);
        Collections.sort(products, new Comparator<ChipProduct>() {
            @Override
            public int compare(ChipProduct p1, ChipProduct p2) {
                return p1.getPrice().compareTo(p2.getPrice());
            }
        });
        return products.get(POSITION_OF_MOST_POPULAR_PRODUCT - 1).getProductId();
    }
}
