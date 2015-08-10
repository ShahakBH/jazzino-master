package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GamblerEnd implements NovomaticEvent {
    private final long winMeter;
    private final long credit;

    public GamblerEnd(long winMeter, long credit) {
        this.winMeter = winMeter;
        this.credit = credit;
    }

    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventGamblerEnd.getNovomaticEventType();
    }

    public long getWinMeter() {
        return winMeter;
    }

    public long getCredit() {
        return credit;
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
        GamblerEnd rhs = (GamblerEnd) obj;
        return new EqualsBuilder()
                .append(this.winMeter, rhs.winMeter)
                .append(this.credit, rhs.credit)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(winMeter)
                .append(credit)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
