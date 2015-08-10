package com.yazino.web.domain.facebook;

import com.restfb.FacebookClient;

public interface FacebookClientFactory {
    FacebookClient getClient(String authToken);
}
