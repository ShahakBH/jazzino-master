package com.yazino.platform.test.game;

import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.generic.variation.Variation;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class GameRuleVariation implements Variation {
    private static final long serialVersionUID = 815635608308472462L;

    public enum Property {
        PROPERTY("default Value");

        Property(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        static Property parse(final String propertyText) {
            notNull(propertyText, "propertyText is null");
            for (Property property : Property.values()) {
                if (format(propertyText).equalsIgnoreCase(property.name())) {
                    return property;
                }
            }
            throw new IllegalArgumentException(String.format("Property %s not recognised", propertyText));
        }

        static String format(final String property) {
            return property.replace(" ", "_");
        }

        private final String defaultValue;

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    private HashMap<Property, String> properties;

    private GameRuleVariation() {
        this.properties = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public GameRuleVariation(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        properties = new HashMap<>();
        for (Map.Entry<String, String> propertyEntry : ((Map<String, String>) document.get("properties")).entrySet()) {
            properties.put(Property.valueOf(propertyEntry.getKey()), propertyEntry.getValue());
        }
    }

    @Override
    public Map<String, Object> toDocument() {
        final Map<String, String> propertyMap = new HashMap<>();
        for (Property property : properties.keySet()) {
            propertyMap.put(property.name(), properties.get(property));
        }

        return new DocumentBuilder()
                .withPrimitiveMapOf("properties", propertyMap)
                .toDocument();
    }

    public static GameRuleVariation withProperties(final Map<String, String> properties) {
        final GameRuleVariation variation = new GameRuleVariation();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            variation.properties.put(Property.parse(property.getKey()), property.getValue());
        }
        return variation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        GameRuleVariation rhs = (GameRuleVariation) obj;
        return new EqualsBuilder()
                .append(this.properties, rhs.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("properties", properties)
                .toString();
    }

}
