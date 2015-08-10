package com.yazino.web.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class ClientContextConverter {
    private static final Logger LOG = LoggerFactory.getLogger(ClientContextConverter.class);

    private ClientContextConverter() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(final String clientContextJson) {
        if (StringUtils.isBlank(clientContextJson)) {
            return new HashMap<>();
        }

        try {
            JsonHelper jsonHelper = new JsonHelper();
            return jsonHelper.deserialize(Map.class, clientContextJson);
        } catch (Exception e) {
            LOG.error("Could not convert clientContext Json please check your json {}", clientContextJson);
            return new HashMap<>();
        }

    }

}
