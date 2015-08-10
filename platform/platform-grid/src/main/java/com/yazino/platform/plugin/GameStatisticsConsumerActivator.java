package com.yazino.platform.plugin;

import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.platform.plugin.statistic.GameStatisticPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

public class GameStatisticsConsumerActivator implements Activator {
    private static final Logger LOG = LoggerFactory.getLogger(GameStatisticsConsumerActivator.class);
    public static final String FILTER = "(&(objectClass=" + GameStatisticConsumer.class.getName() + "))";
    private final GameStatisticPlugin gameStatisticPlugin;
    private BundleContext context;

    public GameStatisticsConsumerActivator(final GameStatisticPlugin gameStatisticPlugin) {
        notNull(gameStatisticPlugin, "gameStatisticPlugin cannot be null");
        this.gameStatisticPlugin = gameStatisticPlugin;
    }

    public void start(final BundleContext bundleContext) throws Exception {
        LOG.debug("Registering listener for game statistics consumer");
        bundleContext.addServiceListener(this, FILTER);
        context = bundleContext;
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        context = null;
    }

    public void serviceChanged(final ServiceEvent serviceEvent) {
        final GameStatisticConsumer gameStatisticConsumer = (GameStatisticConsumer)
                context.getService(serviceEvent.getServiceReference());
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                LOG.debug("Registering game statistics consumer {}", gameStatisticConsumer);
                gameStatisticPlugin.addGameStatisticsConsumer(gameStatisticConsumer);
                break;

            case ServiceEvent.UNREGISTERING:
                LOG.debug("Removing game statistics consumer {}", gameStatisticConsumer);
                gameStatisticPlugin.removeGameStatisticsConsumer(gameStatisticConsumer);
                break;

            default:
                LOG.error("Unknown operation {}", serviceEvent.getType());
        }
    }
}
