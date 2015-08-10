package com.yazino.platform.repository.table;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.game.api.GameMetaData;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;

import java.util.Set;

public interface GameRepository {
    Set<GameTypeInformation> getAvailableGameTypes();

    GameRules getGameRules(String gameTypeId);

    boolean isGameAvailable(String gameTypeId);

    void setGameAvailable(String gameTypeId, boolean isAvailable);

    GameType getGameTypeFor(String gameTypeId);

    GameMetaData getMetaDataFor(String gameTypeId);
}
