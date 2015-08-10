package com.yazino.web.domain.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;

public final class FacebookClientFactoryImpl implements FacebookClientFactory {
    private static final FacebookClientFactoryImpl INSTANCE = new FacebookClientFactoryImpl();

    private FacebookClientFactoryImpl() {
    }

    public static FacebookClientFactoryImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public FacebookClient getClient(final String authToken) {
        return new DefaultFacebookClient(authToken);
    }

}
