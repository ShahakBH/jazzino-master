package com.yazino.platform.payment.dispute;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import org.springframework.stereotype.Service;

@Service
public class FacebookClientFactory {

    public FacebookClient facebookClientFor(final String accessToken) {
        return new DefaultFacebookClient(accessToken);
    }

}
