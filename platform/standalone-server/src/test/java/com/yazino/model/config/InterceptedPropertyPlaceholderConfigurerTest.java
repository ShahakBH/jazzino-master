package com.yazino.model.config;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class InterceptedPropertyPlaceholderConfigurerTest {

    private InterceptedPropertyPlaceholderConfigurer underTest;

    @Before
    public void setUp() {
        underTest = new InterceptedPropertyPlaceholderConfigurer();
    }

    @Test
    public void afterLoadingPropertiesResolvesServerProperties() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put("p2", "v2");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getServerProperties();
        assertEquals(allProps, actual);
    }

    @Test
    public void afterLoadingPropertiesResolvesServerPropertiesExcludesInternal() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put("p2internalp2", "v2");
        allProps.put("p3", "v3");
        Map<String, String> expected = new TreeMap<String, String>();
        expected.put("p1", "v1");
        expected.put("p3", "v3");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getServerProperties();
        assertEquals(expected, actual);
    }

    @Test
    public void afterLoadingPropertiesResolvesServerPropertiesExcludesVariation() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.VARIATION_SUFFIX + "p2", "v2");
        allProps.put("p3", "v3");
        Map<String, String> expected = new TreeMap<String, String>();
        expected.put("p1", "v1");
        expected.put("p3", "v3");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getServerProperties();
        assertEquals(expected, actual);
    }

    @Test
    public void afterLoadingPropertiesResolvesServerPropertiesExcludesFlashProperties() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.FLASHVAR_SUFFIX + "p2", "v2");
        allProps.put("p3", "v3");
        Map<String, String> expected = new TreeMap<String, String>();
        expected.put("p1", "v1");
        expected.put("p3", "v3");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getServerProperties();
        assertEquals(expected, actual);
    }

    @Test
    public void afterLoadingPropertiesResolvesVariationProperties() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.VARIATION_SUFFIX + "p2", "v2");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.VARIATION_SUFFIX + "p3", "v3");
        allProps.put("p4", "v4");
        Map<String, String> expected = new TreeMap<String, String>();
        expected.put("p2", "v2");
        expected.put("p3", "v3");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getVariationProperties();
        assertEquals(expected, actual);
    }

    @Test
    public void afterLoadingPropertiesResolvesFlashvarProperties() throws IOException {
        Map<String, String> allProps = new TreeMap<String, String>();
        allProps.put("p1", "v1");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.FLASHVAR_SUFFIX + "p2", "v2");
        allProps.put(InterceptedPropertyPlaceholderConfigurer.FLASHVAR_SUFFIX + "p3", "v3");
        allProps.put("p4", "v4");
        Map<String, String> expected = new TreeMap<String, String>();
        expected.put("p2", "v2");
        expected.put("p3", "v3");
        underTest.loadProperties(propsFor(allProps));
        final Map<String, String> actual = underTest.getFlashvars();
        assertEquals(expected, actual);
    }

    private Properties propsFor(final Map<String, String> allProps) {
        final Properties properties = new Properties();
        properties.putAll(allProps);
        return properties;
    }
}
