package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class GameVariation implements Serializable {
    private static final long serialVersionUID = -3545865499442565637L;

    private final BigDecimal id;
    private final String gameType;
    private final String name;
    private final Map<String, String> properties;

    public GameVariation(final BigDecimal id,
                         final String gameType,
                         final String name,
                         final Map<String, String> properties) {
        notNull(id, "id may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(name, "name may not be null");
        notNull(properties, "properties may not be null");

        this.id = id;
        this.gameType = gameType;
        this.name = name;
        this.properties = Collections.unmodifiableMap(properties);
    }

    public BigDecimal getId() {
        return id;
    }

    public String getGameType() {
        return gameType;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
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
        final GameVariation rhs = (GameVariation) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(gameType, rhs.gameType)
                .append(name, rhs.name)
                .append(properties, rhs.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 19)
                .append(id)
                .append(gameType)
                .append(name)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(gameType)
                .append(name)
                .append(properties)
                .toString();
    }
}
