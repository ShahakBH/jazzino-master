package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class ObservableTimeOutEventInfo implements Serializable {
    private static final long serialVersionUID = 3883087973719266662L;
    private String type;
    private long millisTillEvent;
    private long initalMillisTillTimeout;

    public ObservableTimeOutEventInfo() {
    }

    public ObservableTimeOutEventInfo(final String type,
                                      final long millisTillEvent,
                                      final long initalMillisTillTimeout) {
        this.type = type;
        this.millisTillEvent = millisTillEvent;
        this.initalMillisTillTimeout = initalMillisTillTimeout;
    }

    public String getType() {
        return type;
    }

    public long getMillisTillEvent() {
        return millisTillEvent;
    }

    public long getInitalMillisTillTimeout() {
        return initalMillisTillTimeout;
    }

    public String[] toArgs() {
        return new String[]{type, String.valueOf(initalMillisTillTimeout), String.valueOf(millisTillEvent)};
    }

    public void setInitalMillisTillTimeout(final long initalMillisTillTimeout) {
        this.initalMillisTillTimeout = initalMillisTillTimeout;
    }

    public void setMillisTillEvent(final long millisTillEvent) {
        this.millisTillEvent = millisTillEvent;
    }

    public void setType(final String type) {
        this.type = type;
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
        final ObservableTimeOutEventInfo rhs = (ObservableTimeOutEventInfo) obj;
        return new EqualsBuilder()
                .append(initalMillisTillTimeout, rhs.initalMillisTillTimeout)
                .append(millisTillEvent, rhs.millisTillEvent)
                .append(type, rhs.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(type)
                .append(millisTillEvent)
                .append(initalMillisTillTimeout)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(type)
                .append(millisTillEvent)
                .append(initalMillisTillTimeout)
                .toString();
    }
}
