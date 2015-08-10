package com.yazino.game.api;

public interface GameInformation {

    Long getGameId();

    Long getIncrement();

    GameStatus getCurrentGame();

    boolean isAddingPlayersPossible();

}
