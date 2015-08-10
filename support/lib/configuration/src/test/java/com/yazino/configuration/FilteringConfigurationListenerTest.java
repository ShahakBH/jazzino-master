package com.yazino.configuration;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FilteringConfigurationListenerTest {

    @Mock
    private ConfigurationPropertyChangeCallback callback;

    private FilteringConfigurationListener underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new FilteringConfigurationListener(callback, "property.one", "property.two");
    }

    @Test(expected = NullPointerException.class)
    public void theListenerCannotBeCreatedWithANullCallback() {
        new FilteringConfigurationListener(null, "property.one");
    }

    @Test(expected = NullPointerException.class)
    public void theListenerCannotBeCreatedWithNullProperties() {
        new FilteringConfigurationListener(callback, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void theListenerCannotBeCreatedWithEmptyProperties() {
        new FilteringConfigurationListener(callback);
    }

    @Test
    public void eventsWithNoPropertyNameArePassedToTheGenericCallback() {
        underTest.configurationChanged(new ConfigurationEvent(this, 0, null, null, false));

        verify(callback).propertiesChanged();
    }

    @Test
    public void eventsWithAnUnfilteredPropertyNameAreIgnored() {
        underTest.configurationChanged(new ConfigurationEvent(this, 0, "property.three", "aValue", false));

        verifyZeroInteractions(callback);
    }

    @Test
    public void eventsWithAFilteredPropertyNameArePassedToTheSpecificCallback() {
        underTest.configurationChanged(new ConfigurationEvent(this, 0, "property.two", "aValue", false));

        verify(callback).propertyChanged("property.two", "aValue");
    }

    @Test
    public void eventsBeforeTheChangeAreIgnored() {
        underTest.configurationChanged(new ConfigurationEvent(this, 0, "property.two", "aValue", true));

        verifyZeroInteractions(callback);
    }

    @Test
    public void nullEventsWithAreIgnored() {
        underTest.configurationChanged(null);

        verifyZeroInteractions(callback);
    }


}
