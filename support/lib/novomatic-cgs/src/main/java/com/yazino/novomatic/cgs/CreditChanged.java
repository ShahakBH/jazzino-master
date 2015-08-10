package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class CreditChanged implements NovomaticEvent {
    private final long credit;
    private final long wallet;

    public long getCredit() {
        return credit;
    }

    public long getWallet() {
        return wallet;
    }

    public CreditChanged(long credit, long wallet) {
        this.credit = credit;
        this.wallet = wallet;
    }

    @Override
    public String getNovomaticEventType() {
        return NovomaticEventType.EventCreditChange.getNovomaticEventType();
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
        CreditChanged rhs = (CreditChanged) obj;
        return new EqualsBuilder()
                .append(this.credit, rhs.credit)
                .append(this.wallet, rhs.wallet)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(credit)
                .append(wallet)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
