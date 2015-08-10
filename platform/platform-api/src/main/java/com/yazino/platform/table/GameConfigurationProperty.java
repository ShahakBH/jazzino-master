package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class GameConfigurationProperty implements Serializable {
    private static final long serialVersionUID = 322782116850808547L;
    private BigDecimal propertyId;
    private String gameId;
    private String propertyName;
    private String propertyValue;

    public GameConfigurationProperty(final BigDecimal propertyId,
                                     final String gameId,
                                     final String propertyName,
                                     final String propertyValue) {
        this.propertyId = propertyId;
        this.gameId = gameId;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public BigDecimal getPropertyId() {
        return propertyId;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
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
        final GameConfigurationProperty rhs = (GameConfigurationProperty) obj;
        return new EqualsBuilder()
                .append(gameId, rhs.gameId)
                .append(propertyId, rhs.propertyId)
                .append(propertyName, rhs.propertyName)
                .append(propertyValue, rhs.propertyValue)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gameId)
                .append(propertyId)
                .append(propertyName)
                .append(propertyValue)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "GameConfigurationProperty{"
                + "propertyId=" + propertyId
                + ", gameId=" + gameId
                + ", propertyName='" + propertyName + '\''
                + ", propertyValue='" + propertyValue + '\''
                + '}';
    }
}
