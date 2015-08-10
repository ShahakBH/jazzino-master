package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GamblerInfo implements NovomaticEvent {
    private final String history;
    private final String symbol;
    private final long winmeter;
    private final long step;

    public String getHistory() {
        return history;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getWinmeter() {
        return winmeter;
    }

    public long getStep() {
        return step;
    }

    public GamblerInfo(String history, String symbol, long winmeter, long step) {

        this.history = history;
        this.symbol = symbol;
        this.winmeter = winmeter;
        this.step = step;
    }

    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventGamblerInfo.getNovomaticEventType();
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
        GamblerInfo rhs = (GamblerInfo) obj;
        return new EqualsBuilder()
                .append(this.history, rhs.history)
                .append(this.symbol, rhs.symbol)
                .append(this.winmeter, rhs.winmeter)
                .append(this.step, rhs.step)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(history)
                .append(symbol)
                .append(winmeter)
                .append(step)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
