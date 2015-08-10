package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class GameConfiguration implements Serializable {
    private static final long serialVersionUID = 2409431287516894684L;
    private String gameId;
    private String shortName;
    private String displayName;
    private Collection<String> aliases;
    private Collection<GameConfigurationProperty> properties;
    private Map<String, GameConfigurationProperty> propertyMap;
    private final int order;

    public GameConfiguration(final String gameId,
                             final String shortName,
                             final String displayName,
                             final Collection<String> aliases,
                             final int order) {
        this(gameId,
            shortName,
            displayName,
            new HashSet<GameConfigurationProperty>(),
            aliases,
            order);
    }

    private GameConfiguration(final String gameId,
                              final String shortName,
                              final String displayName,
                              final Collection<GameConfigurationProperty> properties,
                              final Collection<String> aliases,
                              final int order) {
        notNull(gameId, "gameId cannot be null");
        notBlank(shortName, "shortName cannot be empty");
        notBlank(displayName, "displayName cannot be empty");
        this.gameId = gameId;
        this.shortName = shortName;
        this.displayName = displayName;
        this.properties = properties;
        this.propertyMap = new HashMap<String, GameConfigurationProperty>();
        for (GameConfigurationProperty property : properties) {
            propertyMap.put(property.getPropertyName(), property);
        }
        this.aliases = aliases;
        this.order = order;
    }

    public GameConfiguration withProperties(final Collection<GameConfigurationProperty> newProperties) {
        return new GameConfiguration(gameId, shortName, displayName, newProperties, aliases, order);
    }

    public String getGameId() {
        return gameId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Collection<GameConfigurationProperty> getProperties() {
        return properties;
    }

    public String getProperty(final String propertyName) {
        final GameConfigurationProperty property = propertyMap.get(propertyName);
        if (property != null) {
            return property.getPropertyValue();
        }
        return null;
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final GameConfiguration rhs = (GameConfiguration) obj;
        return new EqualsBuilder()
                .append(displayName, rhs.displayName)
                .append(gameId, rhs.gameId)
                .append(properties, rhs.properties)
                .append(shortName, rhs.shortName)
                .append(aliases, rhs.aliases)
                .append(order, rhs.order)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(displayName)
                .append(gameId)
                .append(properties)
                .append(shortName)
                .append(aliases)
                .append(order)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(displayName)
                .append(gameId)
                .append(properties)
                .append(shortName)
                .append(aliases)
                .append(order)
                .toString();
    }
}
