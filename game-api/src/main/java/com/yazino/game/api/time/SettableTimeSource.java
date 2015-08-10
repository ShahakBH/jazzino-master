package com.yazino.game.api.time;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class SettableTimeSource implements TimeSource, Serializable {
    private static final long serialVersionUID = -4880788382003289703L;

    private long currentMillisSeconds = 0L;

    public SettableTimeSource() {
    }

    public SettableTimeSource(final boolean initToCurrentTime) {
        if (initToCurrentTime) {
            currentMillisSeconds = System.currentTimeMillis();
        }
    }

    public SettableTimeSource(final long currentMillisSeconds) {
        this.currentMillisSeconds = currentMillisSeconds;
    }

    public void setMillis(final long newMilliSeconds) {
        currentMillisSeconds = newMilliSeconds;
    }

    public long getCurrentTimeStamp() {
        return currentMillisSeconds;
    }

    public void addMillis(final long incByMilliSeconds) {
        currentMillisSeconds += incByMilliSeconds;
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
        final SettableTimeSource rhs = (SettableTimeSource) obj;
        return new EqualsBuilder()
                .append(currentMillisSeconds, rhs.currentMillisSeconds)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(currentMillisSeconds)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(currentMillisSeconds)
                .toString();
    }
}
