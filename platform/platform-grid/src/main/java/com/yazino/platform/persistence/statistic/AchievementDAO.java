package com.yazino.platform.persistence.statistic;


import com.yazino.platform.model.statistic.Achievement;

import java.util.Collection;

public interface AchievementDAO {

    Collection<Achievement> findAll();

}
