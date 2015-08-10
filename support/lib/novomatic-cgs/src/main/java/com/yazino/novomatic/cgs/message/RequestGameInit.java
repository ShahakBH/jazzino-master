package com.yazino.novomatic.cgs.message;

import java.util.HashMap;
import java.util.Map;

public class RequestGameInit {

    private static final String TYPE = "req_gmengine_init";
    private final Long gameId;
    private final long walletBalance;

    public RequestGameInit(Long gameId, long walletBalance) {
        this.gameId = gameId;
        this.walletBalance = walletBalance;
    }

    public Map toMap() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("type", TYPE);
        result.put("id", gameId);
        result.put("wallet", walletBalance);
        return result;
    }
}
