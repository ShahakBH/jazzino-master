package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.PurchaseStatus;
import com.yazino.web.util.JsonHelper;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.web.payment.PurchaseStatus.SUCCESS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GooglePurchaseTest {

    @Test
    public void isSerializableAndDeserializable() {
        GooglePurchase purchase = new GooglePurchase();
        purchase.setCanConsume(true);
        purchase.setStatus(SUCCESS);
        purchase.setChips(BigDecimal.valueOf(3000));
        purchase.setCurrencyCode("USD");
        purchase.setErrorMessage("no error");
        purchase.setPrice(BigDecimal.valueOf(5.50));
        purchase.setPurchaseId("purchase id");

        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(purchase);

        GooglePurchase deserialized = jsonHelper.deserialize(GooglePurchase.class, serialized);

        assertThat(purchase, is(deserialized));
    }

    @Test
    public void nullValuesAreNotSerialized() {
        GooglePurchase purchase = new GooglePurchase();

        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(purchase);

        GooglePurchase deserialized = jsonHelper.deserialize(GooglePurchase.class, serialized);

        assertThat(purchase, is(deserialized));
        assertThat(serialized, is("{}"));
    }
}
