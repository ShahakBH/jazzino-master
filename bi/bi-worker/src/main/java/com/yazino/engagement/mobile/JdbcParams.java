package com.yazino.engagement.mobile;

import java.util.HashMap;
import java.util.Map;

final class JdbcParams {

    private JdbcParams() { /* utility class */ }

    public static Map<String, Object> buildParams(Object... args) {
        // NOTE: null values are allowed (unlike ImmutableMap)
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            params.put((String) args[i], args[i + 1]);
        }
        return params;
    }

}
