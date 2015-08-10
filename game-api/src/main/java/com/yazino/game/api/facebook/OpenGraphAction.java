package com.yazino.game.api.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notBlank;

public class OpenGraphAction implements Serializable {
    private static final long serialVersionUID = -1680724801043939832L;

    private final String statisticEventName;
    private final String actionName;
    private final String objectType;
    private final String objectId;

    public OpenGraphAction(final String statisticEventName,
                           final String actionName,
                           final String objectType,
                           final String objectId) {
        notBlank(statisticEventName, "statisticEventName may not be null/blank");
        notBlank(actionName, "actionName may not be null/blank");
        notBlank(objectType, "objectType may not be null/blank");
        notBlank(objectId, "objectId may not be null/blank");

        this.statisticEventName = statisticEventName;
        this.actionName = actionName;
        this.objectType = objectType;
        this.objectId = objectId;
    }

    public String getStatisticEventName() {
        return statisticEventName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
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
        final OpenGraphAction rhs = (OpenGraphAction) obj;
        return new EqualsBuilder()
                .append(statisticEventName, rhs.statisticEventName)
                .append(actionName, rhs.actionName)
                .append(objectType, rhs.objectType)
                .append(objectId, rhs.objectId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(statisticEventName)
                .append(actionName)
                .append(objectType)
                .append(objectId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(statisticEventName)
                .append(actionName)
                .append(objectType)
                .append(objectId)
                .toString();
    }
}
