package com.yazino.web.api.payments;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MostPopularProductPolicyBasedOnPriceTest {
    private MostPopularProductPolicy underTest = new MostPopularProductPolicy();

    @Test
    public void shouldReturnNullWhenProductsListIsNull() {
        assertNull(underTest.findProductIdOfMostPopularProduct(null));
    }

    @Test
    public void shouldReturnNullWhenFewerThan3Products() {
        ArrayList<ChipProduct> chipProducts = Lists.newArrayList(new ChipProduct.ProductBuilder().build(), new ChipProduct.ProductBuilder().build());
        assertNull(underTest.findProductIdOfMostPopularProduct(chipProducts));
    }

    @Test
    public void shouldReturnIdOfThirdCheapestProduct() {
        ArrayList<ChipProduct> chipProducts = Lists.newArrayList(
                new ChipProduct.ProductBuilder().withProductId("id90").withPrice(BigDecimal.valueOf(90)).build(),
                new ChipProduct.ProductBuilder().withProductId("id60").withPrice(BigDecimal.valueOf(60)).build(),
                new ChipProduct.ProductBuilder().withProductId("id50").withPrice(BigDecimal.valueOf(50)).build(),
                new ChipProduct.ProductBuilder().withProductId("id20").withPrice(BigDecimal.valueOf(20)).build());

        assertThat(underTest.findProductIdOfMostPopularProduct(chipProducts), is("id60"));
    }
}
