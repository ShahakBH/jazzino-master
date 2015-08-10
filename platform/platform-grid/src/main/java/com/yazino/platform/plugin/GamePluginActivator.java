package com.yazino.platform.plugin;

import com.yazino.platform.plugin.game.GameRulesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.GameRules;

public class GamePluginActivator implements Activator {
    private static final Logger LOG = LoggerFactory.getLogger(GamePluginActivator.class);
    private static final String FILTER = "(&(objectClass=" + GameRules.class.getName() + "))";
    private GameRulesService gameRulesService;
    private BundleContext bundleContext;

    public GamePluginActivator(final GameRulesService gameRulesService) {
        this.gameRulesService = gameRulesService;
    }

    public void start(final BundleContext startBundleContext) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Game Rules activator started with bundle context %s", startBundleContext));
        }
        startBundleContext.addServiceListener(this, FILTER);
        this.bundleContext = startBundleContext;
    }

    public void stop(final BundleContext stopBundleContext) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Game Rules activator stopped with bundle context %s", stopBundleContext));
        }
        this.bundleContext.removeServiceListener(this);
        this.bundleContext = null;
    }

    public void serviceChanged(final ServiceEvent bundleEvent) {
        final GameRules gameRules = (GameRules) bundleContext.getService(bundleEvent.getServiceReference());
        switch (bundleEvent.getType()) {
            case ServiceEvent.REGISTERED:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registering gameRules " + gameRules);
                }
                gameRulesService.addGameRules(gameRules);
                break;

            case ServiceEvent.UNREGISTERING:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Removing gameRules " + gameRules);
                }
                gameRulesService.removeGameRules(gameRules);
                break;

            default:
                LOG.error(String.format("Unknown operation %s", bundleEvent.getType()));
        }
    }
}
