package com.yazino.web.util;

import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform;
import static org.apache.commons.lang3.Validate.notNull;

@Component
public class LandingUrlRegistry {
    private static final String PROPERTY_NAME = "web.landing-url.%s.%s";

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public LandingUrlRegistry(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public String getLandingUrlForGameAndPlatform(final String gameId,
                                                  final MobilePlatform platform) {
        notNull(gameId, "gameId may not be null");
        notNull(platform, "platform may not be null");

        return yazinoConfiguration.getString(String.format(PROPERTY_NAME, platform, gameId), null);
    }
}
