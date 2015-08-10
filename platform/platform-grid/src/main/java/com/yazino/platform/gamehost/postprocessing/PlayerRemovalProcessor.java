package com.yazino.platform.gamehost.postprocessing;


import com.yazino.platform.model.table.Table;
import com.yazino.game.api.GamePlayerWalletFactory;

public interface PlayerRemovalProcessor {

    void removeAllPlayers(Table table,
                          GamePlayerWalletFactory gamePlayerWalletFactory);

}
