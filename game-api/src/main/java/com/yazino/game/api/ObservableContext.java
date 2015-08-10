package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class ObservableContext {
    private final GamePlayer player;
    private final BigDecimal balance;
    private final boolean skipIfPossible;
    private final long startIncrement;

    public ObservableContext(final GamePlayer player,
                             final BigDecimal balance) {
        this(player, balance, false, 0);
    }

    public ObservableContext(final GamePlayer player,
                             final BigDecimal balance,
                             final boolean skipIfPossible,
                             final long startIncrement) {
        this.player = player;
        this.balance = balance;
        this.skipIfPossible = skipIfPossible;
        this.startIncrement = startIncrement;
    }

    public GamePlayer getPlayer() {
        return player;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isSkipIfPossible() {
        return skipIfPossible;
    }

    public long getStartIncrement() {
        return startIncrement;
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
        final ObservableContext rhs = (ObservableContext) obj;
        return new EqualsBuilder()
                .append(player, rhs.player)
                .append(balance, rhs.balance)
                .append(skipIfPossible, rhs.skipIfPossible)
                .append(startIncrement, rhs.startIncrement)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(player)
                .append(balance)
                .append(skipIfPossible)
                .append(startIncrement)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(player)
                .append(balance)
                .append(skipIfPossible)
                .append(startIncrement)
                .toString();
    }
}
