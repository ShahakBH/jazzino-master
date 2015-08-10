package com.yazino.game.api;

/**
 * This is an interface to allow games a very simple interface to find the wallet
 * for a game. Any required depedencies can be wrapped up in here.
 */
public interface GamePlayerWalletFactory {

    GamePlayerWallet forPlayer(GamePlayer gamePlayer);

}
