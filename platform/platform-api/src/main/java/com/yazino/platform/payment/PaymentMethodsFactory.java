package com.yazino.platform.payment;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;

public final class PaymentMethodsFactory {

    private PaymentMethodsFactory() {
        // utility class
    }

    public static PaymentPreferences.PaymentMethod[] getPaymentMethodsForPlatform(final Platform platform) {

        final PaymentPreferences.PaymentMethod[] paymentMethods;
        switch (platform) {
            case WEB:
                paymentMethods = new PaymentPreferences.PaymentMethod[]{
                        PaymentPreferences.PaymentMethod.CREDITCARD,
                        PaymentPreferences.PaymentMethod.PAYPAL};
                break;
            case IOS:
                paymentMethods = new PaymentPreferences.PaymentMethod[]{PaymentPreferences.PaymentMethod.ITUNES};
                break;
            case FACEBOOK_CANVAS:
                paymentMethods = new PaymentPreferences.PaymentMethod[]{PaymentPreferences.PaymentMethod.FACEBOOK};
                break;
            case ANDROID:
                paymentMethods = new PaymentPreferences.PaymentMethod[]{PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT};
                break;
            case AMAZON:
                paymentMethods = new PaymentPreferences.PaymentMethod[]{PaymentPreferences.PaymentMethod.AMAZON};
                break;
            default:
                throw new RuntimeException("Unsupported platform " + platform);
        }
        return paymentMethods;
    }
}
