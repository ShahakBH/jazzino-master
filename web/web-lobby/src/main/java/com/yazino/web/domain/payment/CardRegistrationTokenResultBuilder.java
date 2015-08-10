package com.yazino.web.domain.payment;

public class CardRegistrationTokenResultBuilder {
    private String registrationURL;
    private String token;
    private boolean test;

    public CardRegistrationTokenResultBuilder withRegistrationURL(final String registrationURL) {
        this.registrationURL = registrationURL;
        return this;
    }

    public CardRegistrationTokenResultBuilder withToken(final String token) {
        this.token = token;
        return this;
    }

    public CardRegistrationTokenResultBuilder withTest(final boolean isTest) {
        this.test = isTest;
        return this;
    }

    public CardRegistrationTokenResult build() {
        return new CardRegistrationTokenResult(token, test, registrationURL);
    }
}