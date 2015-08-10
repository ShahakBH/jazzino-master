package com.yazino.web.domain.payment;

public class CardRegistrationTokenResult {
    private final String token;
    private final boolean isTest;
    private final String registrationURL;

    public String getToken() {
        return token;
    }

    public boolean isTest() {
        return isTest;
    }

    public String getRegistrationURL() {
        return registrationURL;
    }

    CardRegistrationTokenResult(final String token, final boolean isTest, final String registrationURL) {
        this.token = token;
        this.isTest = isTest;
        this.registrationURL = registrationURL;
    }
}
