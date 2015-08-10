package com.yazino.platform.service.tournament;

import java.math.BigDecimal;

public interface AwardTrophyService {
    void awardTrophy(BigDecimal playerId, BigDecimal trophyId);
}
