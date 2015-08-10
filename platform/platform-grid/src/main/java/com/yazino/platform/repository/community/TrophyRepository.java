package com.yazino.platform.repository.community;

import com.yazino.platform.community.Trophy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface TrophyRepository {
    void refreshTrophies();

    Trophy findById(BigDecimal id);

    Collection<Trophy> findForGameType(String gameType);

    List<Trophy> findAll();

    void save(Trophy trophy);

    Trophy findByNameAndGameType(String name, String gameType);

    Collection<Trophy> findByName(String trophyName);
}
