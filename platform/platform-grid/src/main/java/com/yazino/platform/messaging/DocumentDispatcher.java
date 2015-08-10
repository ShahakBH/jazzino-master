package com.yazino.platform.messaging;

import java.math.BigDecimal;
import java.util.Set;

public interface DocumentDispatcher {
    void dispatch(Document document);

    void dispatch(Document document, BigDecimal playerId);

    void dispatch(Document document, Set<BigDecimal> playerIds);

}
