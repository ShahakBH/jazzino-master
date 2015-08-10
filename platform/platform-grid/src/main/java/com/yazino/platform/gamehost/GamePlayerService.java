package com.yazino.platform.gamehost;

import com.yazino.game.api.GamePlayer;

import java.math.BigDecimal;

public interface GamePlayerService {
    GamePlayer getPlayer(BigDecimal playerId);
}
