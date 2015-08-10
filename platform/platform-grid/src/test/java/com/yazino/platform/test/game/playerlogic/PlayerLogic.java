package com.yazino.platform.test.game.playerlogic;

import com.yazino.platform.test.game.gamelogic.GameChangeLogic;
import com.yazino.platform.test.game.status.GamePlayerStatus;
import com.yazino.platform.test.game.status.MockGameContext;
import com.yazino.game.generic.status.GamePlayers;

import java.util.Collection;
import java.util.HashSet;

public class PlayerLogic {
    private GameChangeLogic gameChangeLogic;

    public PlayerLogic(final GameChangeLogic gameChangeLogic) {
        this.gameChangeLogic = gameChangeLogic;
    }

    public GamePlayerStatus getPlayerWithTransactionReference(MockGameContext gameContext, final String reference) {
        for (GamePlayerStatus status : gameContext.getMockGameStatus().getGamePlayers().getPlayers()) {
            if (status.hasTransactionReference(reference)) {
                return status;
            }
        }
        return null;
    }

    public GamePlayers<GamePlayerStatus> removeReference(MockGameContext gameContext, String reference) {
        Collection<GamePlayerStatus> updatedPlayers = new HashSet<GamePlayerStatus>();
        for (GamePlayerStatus status : gameContext.getMockGameStatus().getGamePlayers().getPlayers()) {
            if (status.hasTransactionReference(reference)) {
                updatedPlayers.add(status.removeReference(reference));
            } else {
                updatedPlayers.add(status);
            }
        }
        return new GamePlayers<GamePlayerStatus>(updatedPlayers);
    }
}
