package com.yazino.web.payment.android;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.web.payment.googlecheckout.v3.AndroidProductFactory;
import com.yazino.web.payment.googlecheckout.v3.AndroidStoreProduct;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class AndroidPaymentOptionIdTransformerTest {
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "the product id";

    private AndroidPaymentOptionIdTransformer underTest;

    @Mock
    private AndroidProductFactory androidProductfactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AndroidPaymentOptionIdTransformer(androidProductfactory);
    }

    @Test
    public void shouldReturnNullWhenNoAndroidProductFoundForPaymentOption() {
        Mockito.when(androidProductfactory.getProductFor(GAME_TYPE, aPaymentOption(), GOOGLE_CHECKOUT)).thenReturn(null);

        String transformedId = underTest.transformPaymentOptionId(GAME_TYPE, aPaymentOption(), GOOGLE_CHECKOUT);

        assertNull(transformedId);
    }

    @Test
    public void shouldReturnTransformedId() {
        Mockito.when(androidProductfactory.getProductFor(GAME_TYPE, aPaymentOption(), GOOGLE_CHECKOUT)).thenReturn(aAndroidStoreProduct());

        String transformedId = underTest.transformPaymentOptionId(GAME_TYPE, aPaymentOption(), GOOGLE_CHECKOUT);

        assertThat(transformedId, is(PRODUCT_ID));
    }

    private AndroidStoreProduct aAndroidStoreProduct() {
        AndroidStoreProduct androidStoreProduct = new AndroidStoreProduct();
        androidStoreProduct.setProductId(PRODUCT_ID);
        return androidStoreProduct;
    }

    private PaymentOption aPaymentOption() {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId("payment option id");
        return paymentOption;
    }
}
