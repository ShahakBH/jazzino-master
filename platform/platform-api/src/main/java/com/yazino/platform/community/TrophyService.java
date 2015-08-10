package com.yazino.platform.community;


import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.List;

public interface TrophyService {

    List<Trophy> findAll();

    Trophy findById(@Routing BigDecimal id);

    void update(@Routing("getId") Trophy trophy);

    BigDecimal create(Trophy trophy);

    List<Trophy> findForGameType(String gameType);
}
