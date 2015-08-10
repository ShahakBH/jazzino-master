package com.yazino.bi.operations.persistence.facebook;

import org.springframework.stereotype.Component;

import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;

/**
 * Class generating legacy Facebook client instances
 */
@Component
public final class FacebookExtendedClientFactoryImpl implements FacebookClientFactory {
    private static final FacebookExtendedClientFactoryImpl INSTANCE = new FacebookExtendedClientFactoryImpl();

    /**
     * No default constructor
     */
    private FacebookExtendedClientFactoryImpl() {
    }

    /**
     * Returns the unique instance of this class
     *
     * @return Factory's instance
     */
    public static FacebookExtendedClientFactoryImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public FacebookClient createMapCapableGraphClient(final String accessToken) {
        return new DefaultFacebookClient(accessToken, new DefaultWebRequestor(), new MapsCapableJsonMapper());
    }
}
