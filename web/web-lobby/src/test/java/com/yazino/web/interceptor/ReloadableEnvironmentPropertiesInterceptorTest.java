package com.yazino.web.interceptor;

import com.yazino.configuration.FilteringConfigurationListener;
import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ReloadableEnvironmentPropertiesInterceptorTest {

    private final Map<String, String> definitions = new HashMap<String, String>();
    private YazinoConfiguration yazinoConfiguration;
    private ReloadableEnvironmentPropertiesInterceptor underTest;
    private FilteringConfigurationListener listener;

    @Before
    public void setUp() {
        definitions.put("confA", "key.conf.a");
        yazinoConfiguration = mock(YazinoConfiguration.class);
        ArgumentCaptor<FilteringConfigurationListener> captor = ArgumentCaptor.forClass(FilteringConfigurationListener.class);
        underTest = new ReloadableEnvironmentPropertiesInterceptor(definitions, yazinoConfiguration);
        verify(yazinoConfiguration).addConfigurationListener(captor.capture());
        listener = captor.getValue();
    }

    @Test
    public void shouldReadPropertyFromConfiguration() throws Exception {
        when(yazinoConfiguration.getString("key.conf.a")).thenReturn("value A");
        final ModelAndView mav = new ModelAndView();
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A", mav.getModel().get("confA"));
    }

    @Test
    public void shouldReadPropertyFromConfigurationOnlyOnce() throws Exception {
        when(yazinoConfiguration.getString("key.conf.a")).thenReturn("value A");
        final ModelAndView mav = new ModelAndView();
        underTest.postHandle(null, null, null, mav);
        underTest.postHandle(null, null, null, mav);
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A", mav.getModel().get("confA"));
        verify(yazinoConfiguration, times(1)).getString("key.conf.a");
    }

    @Test
    public void shouldIgnorePropertyNotPresentInConfiguration() throws Exception {
        final ModelAndView mav = new ModelAndView();
        underTest.postHandle(null, null, null, mav);
        assertNull(mav.getModel().get("confA"));
    }

    @Test
    public void shouldRealodProperty() throws Exception {
        when(yazinoConfiguration.getString("key.conf.a")).thenReturn("value A", "value A changed");
        final ModelAndView mav = new ModelAndView();
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A", mav.getModel().get("confA"));
        listener.configurationChanged(new ConfigurationEvent("", 0, "key.conf.a", "ignored new value", false));
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A changed", mav.getModel().get("confA"));
    }

    @Test
    public void shouldRealodProperties() throws Exception {
        when(yazinoConfiguration.getString("key.conf.a")).thenReturn("value A", "value A changed");
        final ModelAndView mav = new ModelAndView();
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A", mav.getModel().get("confA"));
        listener.configurationChanged(new ConfigurationEvent("", 0, null, null, false));
        underTest.postHandle(null, null, null, mav);
        assertEquals("value A changed", mav.getModel().get("confA"));
    }
}
