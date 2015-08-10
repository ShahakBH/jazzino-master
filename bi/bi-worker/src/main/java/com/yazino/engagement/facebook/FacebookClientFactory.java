package com.yazino.engagement.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;

/**
 * There is already a facebookClientFactory in Lobby web and bi-engagement com.yazino.engagement
 * to in order to make this accessible to engagement and lobby web that makes sense.
 * Ideally it should be part of some Facebook package/library so we don't have to have
 * large dependencies on other components. That is why this class has been duplicated.
 * Until we have a chance to create a Facebook-api package
 */

public class FacebookClientFactory {
    private static final FacebookClientFactory INSTANCE = new FacebookClientFactory();

    public FacebookClientFactory() {
    }

    public static FacebookClientFactory getInstance() {
        return INSTANCE;
    }


    public FacebookClient getClient(final String accessToken) {
        return new DefaultFacebookClient(accessToken);
    }

}
