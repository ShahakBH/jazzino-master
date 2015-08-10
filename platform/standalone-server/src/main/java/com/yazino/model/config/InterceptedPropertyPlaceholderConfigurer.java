package com.yazino.model.config;

import com.yazino.model.FlashvarsSource;
import com.yazino.model.VariationPropertiesSource;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class InterceptedPropertyPlaceholderConfigurer
        extends PropertyPlaceholderConfigurer
        implements VariationPropertiesSource, FlashvarsSource {

    public static final String VARIATION_SUFFIX = "standalone-server.variation.";
    public static final String FLASHVAR_SUFFIX = "standalone-server.flashvars.";

    private Properties props;
    private final Map<String, String> variationProperties = new TreeMap<String, String>();
    private final Map<String, String> externalProperties = new TreeMap<String, String>();
    private final Map<String, String> flashvarProperties = new TreeMap<String, String>();

    @Override
    protected void loadProperties(final Properties properties) throws IOException {
        this.props = properties;
        super.loadProperties(this.props);
    }

    public Properties getProperties() {
        return props;
    }

    public Map<String, String> getServerProperties() {
        if (externalProperties.size() == 0) {
            for (String key : props.stringPropertyNames()) {
                if (!key.contains("internal")
                        && !key.startsWith(VARIATION_SUFFIX)
                        && !key.startsWith(FLASHVAR_SUFFIX)) {
                    externalProperties.put(key, props.getProperty(key));
                }
            }
        }
        return externalProperties;
    }

    @Override
    public Map<String, String> getVariationProperties() {
        if (variationProperties.size() == 0) {
            variationProperties.putAll(getFilteredProperties(VARIATION_SUFFIX));
        }
        return variationProperties;
    }

    @Override
    public Map<String, String> getFlashvars() {
        if (flashvarProperties.size() == 0) {
            flashvarProperties.putAll(getFilteredProperties(FLASHVAR_SUFFIX));
        }
        return flashvarProperties;
    }

    private Map<String, String> getFilteredProperties(final String filter) {
        final Map<String, String> result = new HashMap<String, String>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(filter)) {
                final String variationKey = key.replaceAll(filter, "");
                result.put(variationKey, props.getProperty(key));
            }
        }
        return result;
    }
}
