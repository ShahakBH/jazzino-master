package com.yazino.mobile.ws.spring;

import org.junit.Test;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PatternMatchingPropertyConfigurerTest {

    private final PatternMatchingPropertyConfigurer configurer = new PatternMatchingPropertyConfigurer();

    @Test
    public void shouldHandleNullBean() {
        assertNull(configurer.postProcessBeforeInitialization(null, "foo"));
    }

    @Test
    public void shouldOnlyDealWithPatternMatchingPropertyMaps() {
        Object mock = mock(Object.class);
        Object actual = configurer.postProcessBeforeInitialization(mock, "test");
        assertSame(mock, actual);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleNoGroupsInPattern() {
        Properties properties = new Properties();
        String key = "abcdefg";
        properties.setProperty(key, "doo");
        configurer.setTestProperties(properties);
        Pattern pattern = Pattern.compile(key);
        PatternMatchingPropertyMap map = new PatternMatchingPropertyMap(pattern);
        Map<String, String> actual = (Map<String, String>) configurer.postProcessBeforeInitialization(map, "test");
        assertTrue(actual.containsKey(key));
        assertEquals("doo", actual.get(key));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldntAddEntriesIfNoPropertiesMatch() {
        Properties properties = new Properties();
        properties.setProperty("facebook.SLOTS.game", "a");
        properties.setProperty("facebook.BLACKJACK.game", "b");
        properties.setProperty("facebook.TEXAS_HOLDEM.game", "c");
        configurer.setTestProperties(properties);
        PatternMatchingPropertyMap map = new PatternMatchingPropertyMap(Pattern.compile("facebooks\\.(.*)\\.game"));
        Map<String, String> actual = (Map<String, String>) configurer.postProcessBeforeInitialization(map, "test");
        assertTrue(actual.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddKeysToMapWithCorrectValues() {
        Properties properties = new Properties();
        properties.setProperty("facebook.SLOTS.game", "a");
        properties.setProperty("facebook.BLACKJACK.game", "b");
        properties.setProperty("facebook.TEXAS_HOLDEM.game", "c");
        configurer.setTestProperties(properties);
        PatternMatchingPropertyMap map = new PatternMatchingPropertyMap(Pattern.compile("facebook\\.(.*)\\.game"));
        Map<String, String> actual = (Map<String, String>) configurer.postProcessBeforeInitialization(map, "test");
        assertEquals("a", actual.get("SLOTS"));
        assertEquals("b", actual.get("BLACKJACK"));
        assertEquals("c", actual.get("TEXAS_HOLDEM"));
    }

    @Test
    public void shouldntDoAnythingInPostProcessAfterInitialization() {
        Object mock = mock(Object.class);
        Object actual = configurer.postProcessAfterInitialization(mock, "test");
        assertSame(mock, actual);
    }


}
