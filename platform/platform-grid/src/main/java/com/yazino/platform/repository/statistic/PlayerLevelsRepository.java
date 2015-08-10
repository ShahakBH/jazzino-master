package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.PlayerLevels;

import java.math.BigDecimal;

public interface PlayerLevelsRepository {

    PlayerLevels forPlayer(BigDecimal playerId);

    void save(PlayerLevels playerLevels);
}
