package com.yazino.model;

import java.math.BigDecimal;
import java.util.List;

public interface StandalonePlayerService {

    StandalonePlayer findById(BigDecimal playerId);

    List<StandalonePlayer> findAll();

    BigDecimal createPlayer(String name);
}
