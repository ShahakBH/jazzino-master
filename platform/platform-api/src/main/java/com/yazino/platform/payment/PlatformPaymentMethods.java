package com.yazino.platform.payment;

import com.yazino.platform.Platform;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod;

/**
 * I've kept and used PaymentMethodsFactory to keep this change backwards compatible
 */
public final class PlatformPaymentMethods {

    private PlatformPaymentMethods() {
    }

    public static PaymentMethod[] getPaymentMethodForPlatform(final Platform platform) {
        return PaymentMethodsFactory.getPaymentMethodsForPlatform(platform);
    }


    public static Platform getPlatformForPaymentMethod(final PaymentMethod paymentMethod) {
        switch (paymentMethod) {
            case GOOGLE_CHECKOUT:
                return Platform.ANDROID;
            case ITUNES:
                return Platform.IOS;
            case FACEBOOK:
                return Platform.FACEBOOK_CANVAS;
            case CREDITCARD:
                return Platform.WEB;
            case PAYPAL:
                return Platform.WEB;
            case AMAZON:
                return Platform.AMAZON;
            default:
                return null;
        }
    }
}
