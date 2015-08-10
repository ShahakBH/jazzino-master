package com.yazino.platform.bonus;

import java.math.BigDecimal;

public interface BonusService {
    public BonusStatus getBonusStatus(BigDecimal playerId);

    public BonusStatus collectBonus(BigDecimal playerId, final BigDecimal sessionId) throws BonusException;
}
