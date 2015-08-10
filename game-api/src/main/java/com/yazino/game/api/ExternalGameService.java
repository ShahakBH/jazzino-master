package com.yazino.game.api;

import java.math.BigDecimal;

public interface ExternalGameService {

    /**
     * Makes a call to an external service (not maintained by Yazino)
     *
     *
     * @param playerId
     * @param callName the name of the call
     * @param context  the context being used for the call
     * @return unique identifier for this call
     */
    String makeExternalCall(BigDecimal playerId, String callName, Object context);
}
