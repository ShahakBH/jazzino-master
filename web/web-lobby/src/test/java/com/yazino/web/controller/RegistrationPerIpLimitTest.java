package com.yazino.web.controller;

import com.yazino.configuration.FilteringConfigurationListener;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.web.service.HourlyRegistrations;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class RegistrationPerIpLimitTest {

    private static final int LIMIT = 1;

    private HourlyRegistrations hourlyRegistrations;
    private RegistrationPerIpLimit underTest;
    private HttpServletRequest request;
    private YazinoConfiguration configuration;
    private FilteringConfigurationListener configurationListener;

    @Before
    public void setUp() {
        hourlyRegistrations = mock(HourlyRegistrations.class);
        configuration = mock(YazinoConfiguration.class);
        when(configuration.getBoolean(RegistrationPerIpLimit.ACTIVE_CONFIG_KEY)).thenReturn(true);
        when(configuration.getInt(RegistrationPerIpLimit.LIMIT_CONFIG_KEY)).thenReturn(LIMIT);
        request = mock(HttpServletRequest.class);
        underTest = new RegistrationPerIpLimit(configuration, hourlyRegistrations);
        configurationListener = extractConfigurationListener(configuration);
    }

    @Test
    public void shouldCheckIfLimitWasReached_UnderLimit() {
        when(hourlyRegistrations.getRegistrationsFrom(anyString())).thenReturn(LIMIT-1);
        assertFalse(underTest.hasReachedLimit(request));
    }

    @Test
    public void shouldCheckIfLimitWasReached_OverLimit() {
        when(hourlyRegistrations.getRegistrationsFrom(anyString())).thenReturn(LIMIT);
        assertTrue(underTest.hasReachedLimit(request));
    }

    @Test
    public void shouldBeTurnedOffViaConfiguration() {
        when(configuration.getBoolean(RegistrationPerIpLimit.ACTIVE_CONFIG_KEY)).thenReturn(false);
        configurationListener.configurationChanged(new ConfigurationEvent("", 0, null, null, false));
        when(hourlyRegistrations.getRegistrationsFrom(anyString())).thenReturn(50);
        assertFalse(underTest.hasReachedLimit(request));
    }

    @Test
    public void shouldChangeLimitViaConfiguration() {
        when(hourlyRegistrations.getRegistrationsFrom(anyString())).thenReturn(LIMIT + 10);
        assertTrue(underTest.hasReachedLimit(request));
        when(configuration.getInt(RegistrationPerIpLimit.LIMIT_CONFIG_KEY)).thenReturn(LIMIT + 11);
        configurationListener.configurationChanged(new ConfigurationEvent("", 0, null, null, false));
        assertFalse(underTest.hasReachedLimit(request));
    }

    @Test
    public void shouldNotRecordRegistrationIfNotActive(){
        when(configuration.getBoolean(RegistrationPerIpLimit.ACTIVE_CONFIG_KEY)).thenReturn(false);
        configurationListener.configurationChanged(new ConfigurationEvent("", 0, null, null, false));
        underTest.recordRegistration(request);
        verifyZeroInteractions(hourlyRegistrations);
    }

    @Test
    public void shouldRecordRegistration(){
        underTest.recordRegistration(request);
        verify(hourlyRegistrations).incrementRegistrationsFrom(anyString());
    }

    private FilteringConfigurationListener extractConfigurationListener(final YazinoConfiguration configuration) {
        ArgumentCaptor<FilteringConfigurationListener> captor = ArgumentCaptor.forClass(FilteringConfigurationListener.class);
        verify(configuration).addConfigurationListener(captor.capture());
        return captor.getValue();
    }
}
