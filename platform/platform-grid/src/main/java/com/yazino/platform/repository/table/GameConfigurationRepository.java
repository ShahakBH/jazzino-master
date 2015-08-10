package com.yazino.platform.repository.table;

import com.yazino.platform.table.GameConfiguration;

import java.util.Collection;

public interface GameConfigurationRepository {

    void refreshAll();

    GameConfiguration findById(String gameId);

    Collection<GameConfiguration> retrieveAll();
}
