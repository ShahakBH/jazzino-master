package com.yazino.web.domain.social;

import java.math.BigDecimal;

public interface PlayerInformationRetriever {
    PlayerInformationType getType();

    Object retrieveInformation(BigDecimal playerId, String gameType);
}
