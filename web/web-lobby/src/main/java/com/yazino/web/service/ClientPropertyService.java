package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.Environment;
import com.yazino.web.util.MessagingHostResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.Validate.notNull;

@Component
public class ClientPropertyService {
    private static final Logger LOG = LoggerFactory.getLogger(ClientPropertyService.class);

    private static final Set<Platform> VERSIONED_PLATFORMS = newHashSet(Platform.AMAZON, Platform.ANDROID, Platform.IOS);

    private final YazinoConfiguration yazinoConfiguration;
    private final MessagingHostResolver messagingHostResolver;
    private final GameAvailabilityService gameAvailabilityService;
    private final PlayerProfileService playerProfileService;
    private final Environment environment;

    @Autowired
    public ClientPropertyService(final YazinoConfiguration yazinoConfiguration,
                                 final MessagingHostResolver messagingHostResolver,
                                 final GameAvailabilityService gameAvailabilityService,
                                 final PlayerProfileService playerProfileService,
                                 final Environment environment) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(messagingHostResolver, "messagingHostResolver may not be null");
        notNull(gameAvailabilityService, "gameAvailabilityService may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(environment, "environment may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.messagingHostResolver = messagingHostResolver;
        this.gameAvailabilityService = gameAvailabilityService;
        this.playerProfileService = playerProfileService;
        this.environment = environment;
    }

    public Map<String, Object> getBasePropertiesFor(Platform platform) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("server-url", ensureEndsWithSlash(yazinoConfiguration.getString("senet.web.host")));
        properties.put("command-url", ensureEndsWithSlash(yazinoConfiguration.getString("senet.web.command")));
        properties.put("content-url", ensureEndsWithSlash(yazinoConfiguration.getString("senet.web.content")));
        properties.put("permanent-content-url", ensureEndsWithSlash(yazinoConfiguration.getString("senet.web.permanent-content")));
        properties.put("client-url", ensureEndsWithSlash(yazinoConfiguration.getString("senet.web.application-content")));
        properties.put("terms-of-service-url", yazinoConfiguration.getString("terms-of-service.url"));
        properties.put("lightstreamer-protocol", yazinoConfiguration.getString("lightstreamer.protocol"));
        properties.put("lightstreamer-server", yazinoConfiguration.getString("lightstreamer.server"));
        properties.put("lightstreamer-port", yazinoConfiguration.getString("lightstreamer.port"));
        properties.put("lightstreamer-adapter-set", yazinoConfiguration.getString("lightstreamer.adapter-set"));
        properties.put("guest-play-enabled", yazinoConfiguration.getString("guest-play.enabled"));
        properties.put("facebook-canvas", yazinoConfiguration.getString("canvas"));
        properties.put("enabled-gift-types", yazinoConfiguration.getString("gifting.enabled-types"));
        properties.put("facebook-canvas", yazinoConfiguration.getString("canvas"));
        if (Platform.ANDROID.equals(platform)) {
            properties.put("gcm-project-id", yazinoConfiguration.getString("google-cloud-messaging.project-id"));
        }
        return Collections.unmodifiableMap(properties);
    }

    public Map<String, Object> getVersionsFor(Platform platform, String gameType, String clientId) {
        Map<String, Object> properties = new HashMap<String, Object>();
        if (!VERSIONED_PLATFORMS.contains(platform)) {
            // forcing upgrade for WEB is currently meaningless and is unlikely to change
            return properties;
        }
        addVersionProperty(platform, gameType, clientId, properties, "minimum");
        addVersionProperty(platform, gameType, clientId, properties, "latest");
        return properties;
    }

    private void addVersionProperty(Platform platform, String gameType, String clientId, Map<String, Object> properties, String property) {
        String propertyKey = String.format("client.%s.%s.%s.version.%s", platform, gameType, clientId, property);
        String propertyValue = trimToNull(yazinoConfiguration.getString(propertyKey));
        if (propertyValue != null) {
            properties.put(property + "-version", propertyValue);

        } else if (environment.isDevelopment()) {
            LOG.error("Missing or blank configuration (environment.properties): {}", propertyKey);

        } else {
            throw new IllegalStateException("Unsupported version: " + propertyKey);
        }
    }

    public Map<String, Object> getSessionPropertiesFor(LobbySession lobbySession) {
        checkNotNull(lobbySession, "lobbySession cannot be null");

        BigDecimal playerId = lobbySession.getPlayerId();
        Map<String, Object> properties = new HashMap<>();
        properties.put("amqp-host", messagingHostResolver.resolveMessagingHostForPlayer(playerId));
        properties.put("amqp-virtual-host", yazinoConfiguration.getString("strata.rabbitmq.virtualhost"));
        properties.put("amqp-port", yazinoConfiguration.getString("strata.rabbitmq.port"));
        properties.put("player-id", playerId.toPlainString());
        properties.put("player-name", lobbySession.getPlayerName());
        properties.put("tags", lobbySession.getTags());
        properties.put("provider", lobbySession.getAuthProvider().toString());
        properties.put("guest", isGuest(playerId).toString()); // TODO we are using this quick-to-implement but non-performant solution
        // as there is some expectation that guest-play will be abandoned in the near future.  Longer term solution would be to expose
        // the guest-status flag via LobbySession
        return Collections.unmodifiableMap(properties);
    }

    private Boolean isGuest(BigDecimal playerId) {
        PlayerProfile profile = playerProfileService.findByPlayerId(playerId);
        return profile.getGuestStatus() == GuestStatus.GUEST;
    }

    private String ensureEndsWithSlash(String url) {
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + "/";
        }
    }

    public Map<String, Object> getAvailabilityOfGameType(String gameType) {
        Map<String, Object> properties = new HashMap<>();
        GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(gameType);
        properties.put("availability", availability.getAvailability().toString());
        if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
            properties.put("maintenance-starts-at-millis", Long.toString(availability.getMaintenanceStartsAtMillis()));
        }
        return properties;
    }
}
