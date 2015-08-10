package com.yazino.test.game;

import java.math.BigDecimal;

public class PlayerCommand {

    private BigDecimal playerId;
    private String commandType;
    private String[] args;

    public PlayerCommand(final BigDecimal playerId,
                         final String commandType,
                         final String... args) {
        this.playerId = playerId;
        this.commandType = commandType;
        this.args = args;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(final String commandType) {
        this.commandType = commandType;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(final String[] args) {
        this.args = args;
    }
}
