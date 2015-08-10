package com.yazino.web.api.payments;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BestValueProductPolicyTest {
    private BestValueProductPolicy underTest = new BestValueProductPolicy();

    @Test
    public void shouldReturnNullIfProductsListIsNull() {
        assertNull(underTest.findProductIdOfBestValueProduct(null));
    }

    @Test
    public void shouldReturnNullIfProductsListIsEmpty() {
        List<ChipProduct> emptyList = Collections.emptyList();

        assertNull(underTest.findProductIdOfBestValueProduct(emptyList));
    }

    @Test
    public void shouldReturnProductIdOfProductWithBestValue() {
        String thisIsTheBestValue = "id90";
        ArrayList<ChipProduct> chipProducts = Lists.newArrayList(
                // id90 yields 200 chips per unit of currency
                new ChipProduct.ProductBuilder().withProductId(thisIsTheBestValue).withPrice(BigDecimal.valueOf(90)).withChips(BigDecimal.valueOf(18000)).build(),
                // id60 yields 66.66 chips per unit of currency
                new ChipProduct.ProductBuilder().withProductId("id60").withPrice(BigDecimal.valueOf(60)).withChips(BigDecimal.valueOf(4000)).build());

        String productIdOfBestValueProduct = underTest.findProductIdOfBestValueProduct(chipProducts);

        assertThat(productIdOfBestValueProduct, is(thisIsTheBestValue));
    }

    @Test
    public void shouldReturnProductIdOfProductWithBestValueAllowingForPromotedValue() {
        String thisIsTheBestValue = "id30";
        ArrayList<ChipProduct> chipProducts = Lists.newArrayList(
                // id90 yields 200 chips per unit of currency
                new ChipProduct.ProductBuilder().withProductId("id90").withPrice(BigDecimal.valueOf(90)).withChips(BigDecimal.valueOf(18000)).build(),
                // id60 yields 66.66 chips per unit of currency
                new ChipProduct.ProductBuilder().withProductId("id60").withPrice(BigDecimal.valueOf(60)).withChips(BigDecimal.valueOf(4000)).build(),
                // id30 yields 1500 chips per unit of currency, only 10 without promotion
                new ChipProduct.ProductBuilder().withProductId(thisIsTheBestValue).withPrice(BigDecimal.valueOf(30)).withChips(BigDecimal.valueOf(300)).withPromoChips(BigDecimal.valueOf(45000)).build());

        String productIdOfBestValueProduct = underTest.findProductIdOfBestValueProduct(chipProducts);

        assertThat(productIdOfBestValueProduct, is(thisIsTheBestValue));
    }
}
