package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * utility class that describes a game rule variation property. used by game rule variation factory
 * implementations to build actual variation classes.
 */
public final class GameRuleVariationProperty {

    private final String name;
    private final String defaultValue;
    private final GameRuleVariationPropertyType type;
    private final int sortIndex;

    public GameRuleVariationProperty(final GameRuleVariationPropertyType type,
                                     final String name,
                                     final String defaultValue,
                                     final int sortIndex) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.type = type;
        this.sortIndex = sortIndex;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public GameRuleVariationPropertyType getType() {
        return type;
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
        final GameRuleVariationProperty rhs = (GameRuleVariationProperty) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(defaultValue, rhs.defaultValue)
                .append(type, rhs.type)
                .append(sortIndex, rhs.sortIndex)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(name)
                .append(defaultValue)
                .append(type)
                .append(sortIndex)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(name)
                .append(defaultValue)
                .append(type)
                .append(sortIndex)
                .toString();
    }
}
