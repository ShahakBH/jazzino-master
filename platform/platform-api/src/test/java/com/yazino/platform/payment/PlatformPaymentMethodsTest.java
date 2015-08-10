package com.yazino.platform.payment;

import com.yazino.platform.Platform;
import org.junit.Test;

import java.util.Arrays;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;

public class PlatformPaymentMethodsTest {
    @Test
    public void getPaymentMethodForPlatformShouldReturnGoogleCheckoutForAndroid() {
        PaymentMethod[] actualPaymentMethods = PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.ANDROID);
        assertEquals(1, actualPaymentMethods.length);
        assertEquals(GOOGLE_CHECKOUT, actualPaymentMethods[0]);
    }

    @Test
    public void getPaymentMethodForPlatformShouldReturnAmazonForAmazon() {
        PaymentMethod[] actualPaymentMethods = PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.AMAZON);
        assertEquals(1, actualPaymentMethods.length);
        assertEquals(AMAZON, actualPaymentMethods[0]);
    }

    @Test
    public void getPaymentMethodForPlatformShouldReturnIOSForItunes() {
        PaymentMethod[] actualPaymentMethods = PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.IOS);
        assertEquals(1, actualPaymentMethods.length);
        assertEquals(ITUNES, actualPaymentMethods[0]);
    }

    @Test
    public void getPaymentMethodForPlatformShouldReturnFacebook_CanvasForFacebook() {
        PaymentMethod[] actualPaymentMethods = PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.FACEBOOK_CANVAS);
        assertEquals(1, actualPaymentMethods.length);
        assertEquals(FACEBOOK, actualPaymentMethods[0]);
    }

    @Test
    public void getPaymentMethodForPlatformShouldReturnPaypalAndCreditCardForWeb() {
        PaymentMethod[] actualPaymentMethods = PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.WEB);
        assertEquals(2, actualPaymentMethods.length);
        assertThat(Arrays.asList(PlatformPaymentMethods.getPaymentMethodForPlatform(Platform.WEB)), hasItems(CREDITCARD, PAYPAL));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnAndroidIfGoogleCheckout() {
        assertEquals(Platform.ANDROID, PlatformPaymentMethods.getPlatformForPaymentMethod(GOOGLE_CHECKOUT));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnAmazonIfAmazon() {
        assertEquals(Platform.AMAZON, PlatformPaymentMethods.getPlatformForPaymentMethod(AMAZON));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnIOSIfITunes() {
        assertEquals(Platform.IOS, PlatformPaymentMethods.getPlatformForPaymentMethod(ITUNES));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnFacebookIfFacebookCanvas() {
        assertEquals(Platform.FACEBOOK_CANVAS, PlatformPaymentMethods.getPlatformForPaymentMethod(FACEBOOK));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnWebIfCreditCard() {
        assertEquals(Platform.WEB, PlatformPaymentMethods.getPlatformForPaymentMethod(CREDITCARD));
    }

    @Test
    public void getPlatformForPaymentMethodsShouldReturnWebIfPaypal() {
        assertEquals(Platform.WEB, PlatformPaymentMethods.getPlatformForPaymentMethod(PAYPAL));
    }
}
