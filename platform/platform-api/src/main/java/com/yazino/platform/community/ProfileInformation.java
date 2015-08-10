package com.yazino.platform.community;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class ProfileInformation implements Serializable {
    private static final long serialVersionUID = 5978682329692224964L;
    private final PlayerInfo player;
    private int trophies;
    private int medals;
    private final BigDecimal balance;

    public ProfileInformation(final PlayerInfo player,
                              final int trophies,
                              final int medals,
                              final BigDecimal balance) {
        this.player = player;
        this.trophies = trophies;
        this.medals = medals;
        this.balance = balance;
    }

    public int getTrophies() {
        return trophies;
    }

    public int getMedals() {
        return medals;
    }

    public PlayerInfo getPlayer() {
        return player;
    }

    public BigDecimal getBalance() {
        return balance;
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
        final ProfileInformation rhs = (ProfileInformation) obj;
        return new EqualsBuilder()
                .append(medals, rhs.medals)
                .append(trophies, rhs.trophies)
                .append(balance, rhs.balance)
                .append(player, rhs.player)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(medals)
                .append(trophies)
                .append(balance)
                .append(player)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(medals)
                .append(trophies)
                .append(balance)
                .append(player)
                .toString();
    }
}
