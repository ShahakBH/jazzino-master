package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

public class CreditWon implements NovomaticEvent {
    private final List<Long> positions;
    private final Long meter;
    private final String symbol;
    private final Long value;
    private final Long line;

    public List<Long> getPositions() {
        return positions;
    }

    public Long getMeter() {
        return meter;
    }

    public String getSymbol() {
        return symbol;
    }

    public Long getValue() {
        return value;
    }

    public Long getLine() {
        return line;
    }

    public CreditWon(List<Long> positions, Long meter, String symbol, Long value, Long line) {
        this.positions = positions;
        this.meter = meter;
        this.symbol = symbol;
        this.value = value;
        this.line = line;
    }


    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventCreditWin.getNovomaticEventType();
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
        CreditWon rhs = (CreditWon) obj;
        return new EqualsBuilder()
                .append(this.positions, rhs.positions)
                .append(this.meter, rhs.meter)
                .append(this.symbol, rhs.symbol)
                .append(this.value, rhs.value)
                .append(this.line, rhs.line)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(positions)
                .append(meter)
                .append(symbol)
                .append(value)
                .append(line)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
