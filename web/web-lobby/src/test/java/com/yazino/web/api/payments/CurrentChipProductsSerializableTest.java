package com.yazino.web.api.payments;

import com.yazino.web.util.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CurrentChipProductsSerializableTest {
    @Test
    public void isSerializableAndDeserializable() {
        ChipProducts chipProducts = new ChipProducts();
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder()
                .withProductId("productId 1")
                .withCurrencyLabel("$")
                .withLabel("super value")
                .withPrice(BigDecimal.valueOf(87.23))
                .withChips(BigDecimal.valueOf(5000))
                .withPromoChips(BigDecimal.valueOf(15000))
                .withPromoId(1224L)
                .build());
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder()
                .withProductId("productId 2")
                .withCurrencyLabel("$")
                .withLabel("really super value")
                .withPrice(BigDecimal.valueOf(120))
                .withChips(BigDecimal.valueOf(10000))
                .withPromoChips(BigDecimal.valueOf(30000))
                .withPromoId(1224L)
                .build());
        chipProducts.setBestProductId("productId 1");
        chipProducts.setMostPopularProductId("productId 2");


        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(chipProducts);
        System.out.println(serialized);

        ChipProducts deserialized = jsonHelper.deserialize(ChipProducts.class, serialized);

        assertThat(chipProducts, is(deserialized));
    }

    @Test
    public void nullValuesAreNotSerialized() {
        ChipProducts chipProducts = new ChipProducts();
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder()
                .withProductId("productId 1")
                .withCurrencyLabel("$")
                .withLabel("super value")
                .withPrice(BigDecimal.valueOf(87.23))
                .withChips(BigDecimal.valueOf(5000))
                .build());
        chipProducts.addChipProduct(new ChipProduct.ProductBuilder()
                .withProductId("productId 2")
                .withCurrencyLabel("$")
                .withLabel("really super value")
                .withPrice(BigDecimal.valueOf(120))
                .withChips(BigDecimal.valueOf(10000))
                .withPromoChips(BigDecimal.valueOf(30000))
                .withPromoId(1224L)
                .build());

        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(chipProducts);
        System.out.println(serialized);

        ChipProducts deserialized = jsonHelper.deserialize(ChipProducts.class, serialized);

        assertThat(chipProducts, is(deserialized));
    }
}