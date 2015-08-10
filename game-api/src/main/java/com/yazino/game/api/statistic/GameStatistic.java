package com.yazino.game.api.statistic;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

public class GameStatistic implements Serializable {

    private static final long serialVersionUID = 6723465636970925343L;

    private final BigDecimal playerId;
    private final String name;
    private final Map<String, String> properties;

    public GameStatistic(final BigDecimal playerId,
                         final String name) {
        this(playerId, name, Collections.<String, String>emptyMap());
    }

    public GameStatistic(final BigDecimal playerId,
                         final String name,
                         final Map<String, String> properties) {
        this.playerId = playerId;
        this.name = name;
        this.properties = Collections.unmodifiableMap(properties);
    }

    public BigDecimal getPlayerId() {
        return playerId;
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
        final GameStatistic rhs = (GameStatistic) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(playerId, rhs.playerId)
                .append(properties, rhs.properties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(name)
                .append(playerId)
                .append(properties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "[" + name + ", " + playerId + ", " + properties.toString() + "]";
    }
}
