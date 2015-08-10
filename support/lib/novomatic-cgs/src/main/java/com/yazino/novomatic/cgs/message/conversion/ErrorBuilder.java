package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticError;

import java.util.Map;

/**
 * Should handle messages like:
 * <p/>
 * {"type": "rsp_gmengine_error", "code": 101, "descr": "{error,{invalid_gameid,10101}}"}
 */
public class ErrorBuilder {

    public NovomaticError buildFromMap(Map map) {
        return new NovomaticError((Long) map.get("code"), (String) map.get("descr"));
    }
}
