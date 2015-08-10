package com.yazino.game.api.statistic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.Validate.notBlank;

public class StatisticEvent implements Serializable {
    private static final long serialVersionUID = 2984671564426050052L;

    private final String event;
    private final List<Object> parameters = new ArrayList<Object>();
    private final long delay;
    private final int multiplier;

    public StatisticEvent(final String event) {
        notBlank(event, "Event may not be null/blank");

        this.event = event;
        this.delay = 0;
        this.multiplier = 1;
    }

    public StatisticEvent(final String event,
                          final long delay,
                          final int multiplier,
                          final Object... parameters) {
        notBlank(event, "Event may not be null/blank");

        this.event = event;
        this.delay = delay;
        this.multiplier = multiplier;

        if (parameters != null) {
            this.parameters.addAll(Arrays.asList(parameters));
        }
    }

    public String getEvent() {
        return event;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public long getDelay() {
        return delay;
    }

    public int getMultiplier() {
        return multiplier;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final StatisticEvent rhs = (StatisticEvent) obj;
        return new EqualsBuilder()
                .append(event, rhs.event)
                .append(delay, rhs.delay)
                .append(parameters, rhs.parameters)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 39)
                .append(event)
                .append(delay)
                .append(parameters)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
