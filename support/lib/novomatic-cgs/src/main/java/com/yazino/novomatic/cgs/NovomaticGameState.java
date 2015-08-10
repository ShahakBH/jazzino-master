package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class NovomaticGameState implements Serializable {
    private final byte[] internalState;
    private final List<NovomaticEvent> events;

    public NovomaticGameState(byte[] internalState, List<NovomaticEvent> events) {
        this.internalState = internalState;
        this.events = events == null ? Collections.<NovomaticEvent>emptyList(): events;
    }

    public byte[] getInternalState() {
        return internalState;
    }

    public List<NovomaticEvent> getEvents() {
        return events;
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
        NovomaticGameState rhs = (NovomaticGameState) obj;
        return new EqualsBuilder()
                .append(this.internalState, rhs.internalState)
                .append(this.events, rhs.events)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(internalState)
                .append(events)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
