package com.yazino.platform.plugin;

import com.yazino.game.api.facebook.OpenGraphActionProvider;
import com.yazino.platform.processor.statistic.opengraph.DefaultStatisticToActionTransformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

public class OpenGraphActionProviderActivator implements Activator {
    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphActionProviderActivator.class);

    private static final String FILTER = "(&(objectClass=" + OpenGraphActionProvider.class.getName() + "))";

    private final DefaultStatisticToActionTransformer actionTransformer;

    private BundleContext context;

    public OpenGraphActionProviderActivator(final DefaultStatisticToActionTransformer actionTransformer) {
        notNull(actionTransformer, "actionTransformer cannot be null");

        this.actionTransformer = actionTransformer;
    }

    public void start(final BundleContext bundleContext) throws Exception {
        LOG.debug("Registering listener for open graph action provider");

        bundleContext.addServiceListener(this, FILTER);
        context = bundleContext;
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        context.removeServiceListener(this);
        context = null;
    }

    public void serviceChanged(final ServiceEvent serviceEvent) {
        final OpenGraphActionProvider actionProvider = (OpenGraphActionProvider) context.getService(serviceEvent.getServiceReference());
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                LOG.debug("Registering open graph action provider {}", actionProvider);
                actionTransformer.registerActions(actionProvider);
                break;

            case ServiceEvent.UNREGISTERING:
                LOG.debug("Removing open graph action provider {}", actionProvider);
                actionTransformer.unregisterActions(actionProvider);
                break;

            default:
                LOG.error("Unknown operation {}", serviceEvent.getType());
        }
    }
}
