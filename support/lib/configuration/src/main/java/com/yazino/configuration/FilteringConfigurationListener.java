package com.yazino.configuration;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;


/**
 * A listener that filters configuration events by a given set of properties,
 * passing these on via {@link ConfigurationPropertyChangeCallback}.
 */
public class FilteringConfigurationListener implements ConfigurationListener {
    private static final Logger LOG = LoggerFactory.getLogger(FilteringConfigurationListener.class);

    private final Set<String> filteredProperties = new HashSet<String>();

    private final ConfigurationPropertyChangeCallback callback;

    public FilteringConfigurationListener(final ConfigurationPropertyChangeCallback callback,
                                          final String... properties) {
        notNull(callback, "callback may not be null");
        notEmpty(properties, "properties may not be null or empty");

        Collections.addAll(filteredProperties, properties);
        this.callback = callback;
    }

    @Override
    public void configurationChanged(final ConfigurationEvent event) {
        if (event == null || event.isBeforeUpdate()) {
            LOG.debug("Ignoring event {}", event);
            return;
        }

        if (event.getPropertyName() == null) {
            LOG.debug("Calling propertiesChanged");
            callback.propertiesChanged();

        } else if (filteredProperties.contains(event.getPropertyName())) {
            LOG.debug("Calling propertyChanged({}, {})", event.getPropertyName(), event.getPropertyValue());
            callback.propertyChanged(event.getPropertyName(), event.getPropertyValue());

        } else {
            LOG.debug("Ignoring property {}", event.getPropertyName());
        }
    }
}
