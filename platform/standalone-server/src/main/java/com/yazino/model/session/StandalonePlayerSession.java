package com.yazino.model.session;

import java.math.BigDecimal;

public class StandalonePlayerSession {

    private BigDecimal playerId;
    private String name;

    public void setPlayer(final BigDecimal aPlayerId,
                          final String playerName) {
        this.playerId = aPlayerId;
        this.name = playerName;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public boolean isActive() {
        return playerId != null;
    }

    public String getName() {
        return name;
    }
}
