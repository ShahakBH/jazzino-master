package com.yazino.web.api.payments;

import com.yazino.web.util.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ChipProductSerializableTest {
    @Test
    public void isSerializableAndDeserializable() {
        ChipProduct chipProduct = new ChipProduct.ProductBuilder()
                .withProductId("productId")
                .withCurrencyLabel("$")
                .withLabel("super value")
                .withPrice(BigDecimal.valueOf(87))
                .withChips(BigDecimal.valueOf(5000))
                .withPromoChips(BigDecimal.valueOf(15000))
                .withPromoId(1224L)
                .build();
        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(chipProduct);
        System.out.println(serialized);

        ChipProduct deserialized = jsonHelper.deserialize(ChipProduct.class, serialized);

        assertThat(chipProduct, is(deserialized));
    }

    @Test
    public void nullValuesAreNotSerialized() {
        ChipProduct chipProduct = new ChipProduct.ProductBuilder()
                .withProductId("productId")
                .withCurrencyLabel("$")
                .withLabel("super value")
                .withPrice(null)
                .withChips(BigDecimal.valueOf(5000))
                .withPromoChips(null)
                .withPromoId(null)
                .build();
        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(chipProduct);
        System.out.println(serialized);

        ChipProduct deserialized = jsonHelper.deserialize(ChipProduct.class, serialized);

        assertThat(chipProduct, is(deserialized));
    }
}
