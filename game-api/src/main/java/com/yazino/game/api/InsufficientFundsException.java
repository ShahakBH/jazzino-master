package com.yazino.game.api;

public class InsufficientFundsException extends GameException {
    private static final long serialVersionUID = 4629713233545401771L;

    public InsufficientFundsException() {
        super(new ParameterisedMessage("Insufficient funds"));
    }
}
