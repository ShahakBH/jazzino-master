package com.yazino.platform.plugin.game;

import com.yazino.game.api.GameRules;

public interface GameRulesService {

    void addGameRules(GameRules rules);

    void removeGameRules(GameRules gameRules);
}
