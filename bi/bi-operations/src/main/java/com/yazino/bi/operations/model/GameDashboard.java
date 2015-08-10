package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;

public class GameDashboard extends Dashboard {
    private final List<String> gameDetails;

    public GameDashboard(final List<Map<String, Object>> results,
                         final Map<String, String> fields,
                         final Map<String, String> fieldTypes,
                         final List<String> gameDetails) {
        super(PlayerDashboard.GAME, results, fields, fieldTypes);

        this.gameDetails = gameDetails;
    }

    public List<String> getGameDetails() {
        return gameDetails;
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
        final GameDashboard rhs = (GameDashboard) obj;
        return new EqualsBuilder()
                .append(gameDetails, rhs.gameDetails)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameDetails)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(gameDetails)
                .toString();
    }
}
