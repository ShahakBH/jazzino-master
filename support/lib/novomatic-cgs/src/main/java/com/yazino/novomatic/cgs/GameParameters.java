package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GameParameters implements NovomaticEvent {

    private final SelectableValue lines;
    private final SelectableValue betPlacement;
    private final SelectableValue denominationInfo;
    private final Long credit;
    private final Long wallet;

    public SelectableValue getLines() {
        return lines;
    }

    public SelectableValue getBetPlacement() {
        return betPlacement;
    }

    public Long getCredit() {
        return credit;
    }

    public Long getWallet() {
        return wallet;
    }

    public SelectableValue getDenominationInfo() {
        return denominationInfo;
    }

    public GameParameters(SelectableValue lines, SelectableValue betPlacement, final SelectableValue denominationInfo, Long credit, Long wallet) {
        this.lines = lines;
        this.betPlacement = betPlacement;
        this.denominationInfo = denominationInfo;
        this.credit = credit;
        this.wallet = wallet;
    }

    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventParametersChange.getNovomaticEventType();
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
        GameParameters rhs = (GameParameters) obj;
        return new EqualsBuilder()
                .append(this.lines, rhs.lines)
                .append(this.betPlacement, rhs.betPlacement)
                .append(this.credit, rhs.credit)
                .append(this.wallet, rhs.wallet)
                .append(this.denominationInfo, rhs.denominationInfo)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(lines)
                .append(betPlacement)
                .append(credit)
                .append(wallet)
                .append(denominationInfo)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
