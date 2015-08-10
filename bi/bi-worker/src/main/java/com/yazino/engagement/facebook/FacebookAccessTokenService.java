package com.yazino.engagement.facebook;

import com.googlecode.ehcache.annotations.Cacheable;
import com.restfb.WebRequestor;
import com.yazino.engagement.campaign.AccessTokenException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;

import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType;

/**
 * This class is responsible for fetching Application Access Tokens for Facebook Applications
 */
@Service
public class FacebookAccessTokenService {
    public static final int OK = 200;

    /* Access token request url */
    private static final String ACCESS_TOKEN_REQUEST_URI_FORMAT = "https://graph.facebook.com/oauth/access_token"
            + "?client_id=%s&client_secret=%s&grant_type=client_credentials";

    private final WebRequestor webRequestor;

    private final FacebookConfiguration facebookConfiguration;

    // for cglib
    protected FacebookAccessTokenService() {
        webRequestor = null;
        facebookConfiguration = null;
    }

    @Autowired
    public FacebookAccessTokenService(
            final WebRequestor webRequestor,
            @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration) {
        this.webRequestor = webRequestor;
        this.facebookConfiguration = facebookConfiguration;
    }

    @Cacheable(cacheName = "facebookApplicationAccessTokenCache", selfPopulating = true)//this is per application NOT per player
    public String fetchApplicationAccessToken(final String gameType) throws AccessTokenException {
        Validate.notNull(gameType);

        final FacebookAppConfiguration appConfig = facebookConfiguration.getAppConfigFor(gameType, ApplicationType.CANVAS, MatchType.STRICT);
        if (appConfig == null) {
            throw new AccessTokenException("unknown game type: " + gameType);
        }
        return fetchApplicationAccessToken(appConfig.getApplicationId(), appConfig.getSecretKey());
    }

//    @Cacheable(cacheName = "facebookApplicationAccessTokenCache", selfPopulating = true)
    String fetchApplicationAccessToken(final String clientId, final String clientSecret)
            throws AccessTokenException {
        Validate.notNull(clientId);
        Validate.notNull(clientSecret);

        final WebRequestor.Response response = requestAccessToken(clientId, clientSecret);
        return extractAccessTokenFromResponse(clientId, clientSecret, response);
    }

    private WebRequestor.Response requestAccessToken(final String clientId,
                                                     final String clientSecret) throws AccessTokenException {
        try {
            return webRequestor.executeGet(String.format(ACCESS_TOKEN_REQUEST_URI_FORMAT, clientId, clientSecret));
        } catch (IOException e) {
            throw new AccessTokenException(String.format(
                    "Failed to get facebook access token for clientId=%s, clientSecret=%s",
                    clientId, clientSecret), e);
        }
    }

    private String extractAccessTokenFromResponse(final String clientId,
                                                  final String clientSecret,
                                                  final WebRequestor.Response response) throws AccessTokenException {
        final String responseBody = response.getBody();
        if (response.getStatusCode() == OK) {
            return responseBody.split("=")[1];
        } else {
            throw new AccessTokenException(
                    String.format("Failed to get facebook access token for clientId=%s, clientSecret=%s, response=%s",
                            clientId, clientSecret, responseBody));
        }
    }
}
