package com.yazino.web.service;

import java.math.BigDecimal;
import java.util.Set;

public interface PlayerDetailsLobbyService {
    Set<BigDecimal> getFriends(BigDecimal playerId);
}
