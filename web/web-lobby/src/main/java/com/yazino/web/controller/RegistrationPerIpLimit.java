package com.yazino.web.controller;

import com.yazino.configuration.ConfigurationPropertyChangeCallback;
import com.yazino.configuration.FilteringConfigurationListener;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.web.payment.creditcard.IpAddressResolver;
import com.yazino.web.service.HourlyRegistrations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class RegistrationPerIpLimit {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationPerIpLimit.class);

    static final String ACTIVE_CONFIG_KEY = "strata.web.registration-limit-enabled";
    static final String LIMIT_CONFIG_KEY = "strata.web.registration-limit";

    private final YazinoConfiguration configuration;

    private int hourlyLimit;
    private boolean active;

    private final HourlyRegistrations hourlyRegistrations;

    @Autowired
    public RegistrationPerIpLimit(final YazinoConfiguration configuration,
                                  final HourlyRegistrations hourlyRegistrations) {
        this.configuration = configuration;
        configuration.addConfigurationListener(
                new FilteringConfigurationListener(new ConfigurationCallback(), ACTIVE_CONFIG_KEY, LIMIT_CONFIG_KEY));
        updateConfiguration();
        this.hourlyRegistrations = hourlyRegistrations;
    }

    public boolean hasReachedLimit(final HttpServletRequest request) {
        if (!active) {
            LOG.debug("Registration limit verification is disabled");
            return false;
        }
        final String ipAddress = IpAddressResolver.resolveFor(request).getHostAddress();
        final int registrations = hourlyRegistrations.getRegistrationsFrom(ipAddress);
        final boolean reachedLimit = registrations >= hourlyLimit;
        LOG.debug("Address: {}, registrations: {}. Reached limit ({}) ? {}",
                new Object[]{ipAddress, registrations, hourlyLimit, reachedLimit});
        if (reachedLimit) {
            LOG.warn("Limit of {} hourly registrations for IP {} was reached.", hourlyLimit, ipAddress);
        }
        return reachedLimit;
    }

    public void recordRegistration(final HttpServletRequest request) {
        if (!active) {
            LOG.debug("Registration limit verification is disabled. Ignoring new registration.");
            return;
        }
        final String ipAddress = IpAddressResolver.resolveFor(request).getHostAddress();
        hourlyRegistrations.incrementRegistrationsFrom(ipAddress);
    }

    private void updateConfiguration() {
        active = configuration.getBoolean(ACTIVE_CONFIG_KEY);
        hourlyLimit = configuration.getInt(LIMIT_CONFIG_KEY);
        LOG.debug("Updating configuration (active? {}, hourly limit: {}", active, hourlyLimit);
    }

    private class ConfigurationCallback implements ConfigurationPropertyChangeCallback {
        @Override
        public void propertyChanged(final String propertyName, final Object propertyValue) {
            updateConfiguration();
        }

        @Override
        public void propertiesChanged() {
            updateConfiguration();
        }
    }
}
