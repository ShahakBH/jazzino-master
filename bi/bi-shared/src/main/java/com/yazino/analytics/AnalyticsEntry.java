package com.yazino.analytics;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsEntry {
    private int eventId;
    private String type;
    private String sessionId;
    private String label;
    private String value;
    private String action;
    private int delta;

    @JsonCreator
    public AnalyticsEntry(@JsonProperty("eventId") final int eventId,
                          @JsonProperty("type") final String type,
                          @JsonProperty("sessionId") final String sessionId,
                          @JsonProperty("label") final String label,
                          @JsonProperty("value") final String value,
                          @JsonProperty("action") final String action,
                          @JsonProperty("delta") final int delta) {

        this.eventId = eventId;
        this.type = type;
        this.sessionId = sessionId;
        this.sessionId = sessionId;
        this.label = label;
        this.value = value;
        this.action = action;
        this.delta = delta;
    }

    public int getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getAction() {
        return action;
    }

    public int getDelta() {
        return delta;
    }

    public DateTime getTimestampMinusDelta(final DateTime timestamp) {
        return timestamp.minusMillis(delta);
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
        AnalyticsEntry rhs = (AnalyticsEntry) obj;
        return new EqualsBuilder()
                .append(this.eventId, rhs.eventId)
                .append(this.type, rhs.type)
                .append(this.sessionId, rhs.sessionId)
                .append(this.label, rhs.label)
                .append(this.value, rhs.value)
                .append(this.action, rhs.action)
                .append(this.delta, rhs.delta)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(eventId)
                .append(type)
                .append(sessionId)
                .append(label)
                .append(value)
                .append(action)
                .append(delta)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("eventId", eventId)
                .append("type", type)
                .append("sessionId", sessionId)
                .append("label", label)
                .append("value", value)
                .append("action", action)
                .append("delta", delta)
                .toString();
    }
}
