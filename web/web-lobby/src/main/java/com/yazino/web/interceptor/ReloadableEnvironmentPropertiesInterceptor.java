package com.yazino.web.interceptor;

import com.yazino.configuration.ConfigurationPropertyChangeCallback;
import com.yazino.configuration.FilteringConfigurationListener;
import com.yazino.configuration.YazinoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

public class ReloadableEnvironmentPropertiesInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ReloadableEnvironmentPropertiesInterceptor.class);

    private final Map<String, String> environmentProperties = new ConcurrentHashMap<String, String>();

    private final Map<String, String> propertyDefinitions;
    private final YazinoConfiguration yazinoConfiguration;

    public ReloadableEnvironmentPropertiesInterceptor(final Map<String, String> propertyDefinitions,
                                                      final YazinoConfiguration yazinoConfiguration) {
        notNull(propertyDefinitions, "environmentProperties is null");
        notNull(yazinoConfiguration, "yazinoConfiguration is null");
        this.propertyDefinitions = propertyDefinitions;
        this.yazinoConfiguration = yazinoConfiguration;

        final String[] properties = propertyDefinitions.values().toArray(new String[propertyDefinitions.size()]);

        final FilteringConfigurationListener configurationListener
                = new FilteringConfigurationListener(new ConfigurationCallback(), properties);
        yazinoConfiguration.addConfigurationListener(configurationListener);
    }

    private void reloadConfiguration() {
        LOG.debug("Reloading configuration");
        for (Map.Entry<String, String> definition : propertyDefinitions.entrySet()) {
            final String configValue = yazinoConfiguration.getString(definition.getValue());
            if (configValue == null) {
                LOG.warn("Configuration " + definition.getValue() + " not present.");
                continue;
            }
            environmentProperties.put(definition.getKey(), configValue);
        }
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (environmentProperties.size() == 0) {
            reloadConfiguration();
        }
        if (modelAndView != null) {
            for (Map.Entry<String, String> entry : environmentProperties.entrySet()) {
                modelAndView.addObject(entry.getKey(), entry.getValue());
            }
        }
        super.postHandle(request, response, handler, modelAndView);
    }

    private class ConfigurationCallback implements ConfigurationPropertyChangeCallback {
        @Override
        public void propertyChanged(final String propertyName, final Object propertyValue) {
            reloadConfiguration();
        }

        @Override
        public void propertiesChanged() {
            reloadConfiguration();
        }
    }
}
