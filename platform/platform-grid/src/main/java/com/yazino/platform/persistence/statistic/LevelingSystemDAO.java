package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.LevelingSystem;

import java.util.Collection;

public interface LevelingSystemDAO {

    Collection<LevelingSystem> findAll();
}
