package utils;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

// for helping build namedParamJdbcTemplate params
public class ParamBuilder extends HashMap<String, Object> {
    public static ParamBuilder params() {
        return new ParamBuilder();
    }

    public ParamBuilder campaignId(Long campId) {
        this.put("campaignId", campId);
        return this;
    }

    public ParamBuilder campaignRunId(final Long campRunId) {
        this.put("campaignRunId", campRunId);
        return this;
    }

    public ParamBuilder promoId(final Long promoId) {
        this.put("promoId", promoId);
        return this;
    }

    public static ImmutableMap<String, Object> emptyParams() {
        return ImmutableMap.of();
    }

    public ParamBuilder name(final String name) {
        put("name", name);
        return this;
    }

    public ParamBuilder param(String paramName, Object paramValue) {
        put(paramName, paramValue);
        return this;
    }
}