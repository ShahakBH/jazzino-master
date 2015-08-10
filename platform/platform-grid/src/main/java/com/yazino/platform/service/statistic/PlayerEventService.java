package com.yazino.platform.service.statistic;

import java.math.BigDecimal;

public interface PlayerEventService {
    void publishNewLevel(BigDecimal playerId, String gameType, int level, BigDecimal bonusAmount);
}
