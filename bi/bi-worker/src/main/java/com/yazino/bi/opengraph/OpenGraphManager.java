package com.yazino.bi.opengraph;

import com.yazino.platform.opengraph.OpenGraphAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.math.BigInteger;

@Component
public class OpenGraphManager {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphManager.class);
    @Value("${opengraph.ignores.missing.facebook.properties}")
    private boolean ignoreMissingProperties;

    private AccessTokenStore accessTokenStore;
    private OpenGraphHttpInvoker openGraphHttpInvoker;
    private final FacebookConfiguration facebookConfiguration;

    @Autowired
    public OpenGraphManager(final FacebookConfiguration facebookConfiguration,
                            final AccessTokenStore accessTokenStore,
                            final OpenGraphHttpInvoker openGraphHttpInvoker) {
        this.facebookConfiguration = facebookConfiguration;
        this.accessTokenStore = accessTokenStore;
        this.openGraphHttpInvoker = openGraphHttpInvoker;
    }

    public void publishAction(final OpenGraphAction action,
                              final BigInteger playerId,
                              final String gameType) throws IOException {
        if (action == null) {
            LOG.warn("Null action given for {} and player id:{}", gameType, playerId);
            return;
        }

        final String appNamespace = lookupAppNamespace(gameType);
        if (appNamespace == null) {
            LOG.warn("Couldn't find namespace for gametype: {}", gameType);
            return;
        }

        final AccessTokenStore.Key key = new AccessTokenStore.Key(playerId, gameType);
        String accessToken = null;
        if (accessTokenStore.findByKey(key) != null) {
            accessToken = accessTokenStore.findByKey(key).getAccessToken();
        }
        if (accessToken == null) {
            LOG.info("Couldn't find accessToken for playerId: {} gametype: {}", playerId, gameType);
            return;
        }

        if (!openGraphHttpInvoker.hasPermission(accessToken)) {
            LOG.warn("Doesn't have permission to publish: {} this could be as it's cached.", accessToken);
            return;
        }

        try {
            openGraphHttpInvoker.publishAction(accessToken, action, appNamespace);
        } catch (InvalidAccessTokenException e) {
            LOG.warn("Publish failed due to invalid Token: {}", key);
            accessTokenStore.invalidateToken(key);
        }
    }

    private String lookupAppNamespace(final String gameType) {
        final String appNamespace = facebookConfiguration.getAppConfigFor(gameType,
                FacebookConfiguration.ApplicationType.CANVAS,
                FacebookConfiguration.MatchType.STRICT).getAppName();
        if (appNamespace == null) {
            if (ignoreMissingProperties) {
                return null;
            } else {
                throw new IllegalArgumentException("Missing worker property 'facebook.<appname>.appName' for gametype" + gameType);
            }
        }
        return appNamespace;
    }
}
