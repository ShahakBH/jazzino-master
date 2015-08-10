package com.yazino.bi.operations.persistence.facebook;

import com.restfb.FacebookClient;

/**
 * Describes how the facebook API client factory should work
 */
public interface FacebookClientFactory {
    FacebookClient createMapCapableGraphClient(String accessToken);
}
