package com.yazino.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class StandalonePlayer {
    private final BigDecimal playerId;
    private final String name;
    private BigDecimal chips;

    public StandalonePlayer(final BigDecimal playerId, final String name, final BigDecimal chips) {
        this.playerId = playerId;
        this.name = name;
        this.chips = chips;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public void setChips(final BigDecimal chips) {
        this.chips = chips;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(name)
                .append(chips)
                .toString();
    }

}
