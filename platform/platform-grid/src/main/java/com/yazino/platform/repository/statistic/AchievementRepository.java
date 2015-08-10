package com.yazino.platform.repository.statistic;


import com.yazino.platform.model.statistic.Achievement;

import java.util.Collection;

public interface AchievementRepository {

    Collection<Achievement> findAll();

    Collection<Achievement> findByGameType(String gameType);

    void refreshDefinitions();
}
