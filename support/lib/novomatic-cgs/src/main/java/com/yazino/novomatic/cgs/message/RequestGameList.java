package com.yazino.novomatic.cgs.message;

import java.util.HashMap;
import java.util.Map;

public class RequestGameList {
    private static Map<String, String> AS_MAP = new HashMap<String, String>();

    static {
        String TYPE = "req_gmengine_list_descr";
        AS_MAP.put("type", TYPE);
    }

    public Map toMap() {
        return AS_MAP;
    }

}
