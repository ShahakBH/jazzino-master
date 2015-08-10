package com.yazino.platform.plugin;

import com.yazino.platform.plugin.game.GameRulesService;
import com.yazino.platform.plugin.statistic.GameStatisticPlugin;
import com.yazino.platform.processor.statistic.opengraph.DefaultStatisticToActionTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * By default OSGi only provides java.* packages for 'free' any other packages (like javax.*) should usually be imported
 */
@Service
public class GamePluginManager {
    private static final Logger LOG = LoggerFactory.getLogger(GamePluginManager.class);

    private static final String GAME_API_VERSION = "1.4.0";
    private static final String[] EXPOSED_PACKAGES = new String[]{
            "com.yazino.game.api; version=" + GAME_API_VERSION,
            "com.yazino.game.api.document; version=" + GAME_API_VERSION,
            "com.yazino.game.api.facebook; version=" + GAME_API_VERSION,
            "com.yazino.game.api.statistic; version=" + GAME_API_VERSION,
            "com.yazino.game.api.time; version=" + GAME_API_VERSION,
            "com.yazino.novomatic.cgs; version=1.0.0",
            "org.apache.commons.logging; version=1.7.5",
            "org.slf4j; version=1.7.5",
            "sun.reflect.generics.reflectiveObjects",
            "org.osgi.framework; version=4.0.3",
            "javax.*",
            "org.w3c.*",
            "org.xml.*"
    };
    private final PluginManager pluginManager;

    @Autowired
    public GamePluginManager(final GameRulesService gameRulesService,
                             final GameStatisticPlugin gameStatisticPlugin,
                             final DefaultStatisticToActionTransformer actionTransformer,
                             @Value("${strata.games.plugin-directory}") final String pluginDirectory,
                             @Value("${strata.games.cache-directory}") final String cacheDirectory) throws Exception {
        notNull(gameRulesService, "gameRulesService cannot be null");
        notNull(gameStatisticPlugin, "gameStatisticPlugin cannot be null");
        notNull(actionTransformer, "actionTransformer cannot be null");
        notNull(pluginDirectory, "pluginDirectory may not be null");
        notNull(cacheDirectory, "cacheDirectory may not be null");

        LOG.debug("Starting GamePlugin Manager for {}", pluginDirectory);

        this.pluginManager = new PluginFramework(
                pluginDirectory,
                cacheDirectory,
                EXPOSED_PACKAGES,
                new org.slf4j.osgi.logservice.impl.Activator(),
                new GamePluginActivator(gameRulesService),
                new GameStatisticsConsumerActivator(gameStatisticPlugin),
                new GameStatisticsProducerActivator(gameStatisticPlugin),
                new OpenGraphActionProviderActivator(actionTransformer)
        ).init();
        pluginManager.syncPlugins();

        LOG.info("GamePlugin Manager started and plugins synced");
    }

    public byte[] serialise(final Object object) {
        return pluginManager.serialise(object);
    }

    public Object deserialise(final byte[] serialisedObject) {
        return pluginManager.deserialise(serialisedObject);
    }

    public void publishPlugin(final String filename) throws IOException {
        notNull(filename, "filename ID cannot be null");
        LOG.debug("Publishing game JAR {}", filename);
        pluginManager.syncPlugin(filename);
    }
}
