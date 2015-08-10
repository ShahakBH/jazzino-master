package com.yazino.platform.plugin;

import com.yazino.game.api.statistic.GameStatisticProducer;
import com.yazino.platform.plugin.statistic.GameStatisticPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

public class GameStatisticsProducerActivator implements Activator {
    private static final Logger LOG = LoggerFactory.getLogger(GameStatisticsProducerActivator.class);
    private static final String FILTER = "(&(objectClass=" + GameStatisticProducer.class.getName() + "))";
    private GameStatisticPlugin gameStatisticPlugin;
    private BundleContext context;

    public GameStatisticsProducerActivator(final GameStatisticPlugin gameStatisticPlugin) {
        notNull(gameStatisticPlugin, "gameStatisticPlugin cannot be null");
        this.gameStatisticPlugin = gameStatisticPlugin;
    }

    public void start(final BundleContext bundleContext) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registering listener for game statistics producer");
        }
        bundleContext.addServiceListener(this, FILTER);
        context = bundleContext;
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        context.removeServiceListener(this);
        context = null;
    }

    public void serviceChanged(final ServiceEvent serviceEvent) {
        final GameStatisticProducer gameStatisticProducer = (GameStatisticProducer)
                context.getService(serviceEvent.getServiceReference());
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                LOG.debug("Registering game statistics producer {}", gameStatisticProducer);
                gameStatisticPlugin.addGameStatisticsProducer(gameStatisticProducer);
                break;

            case ServiceEvent.UNREGISTERING:
                LOG.debug("Removing game statistics producer {}", gameStatisticProducer);
                gameStatisticPlugin.removeGameStatisticsProducer(gameStatisticProducer);
                break;

            default:
                LOG.error("Unknown operation {}", serviceEvent.getType());
        }
    }
}
