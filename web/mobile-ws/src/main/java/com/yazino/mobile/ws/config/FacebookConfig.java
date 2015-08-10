package com.yazino.mobile.ws.config;

import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.Map;

public class FacebookConfig {

    private Map<String, String> mApplicationIds = Collections.emptyMap();

    public Map<String, String> getApplicationIds() {
        return mApplicationIds;
    }

    public void setApplicationIds(final Map<String, String> applicationIds) {
        Validate.notNull(applicationIds);
        mApplicationIds = applicationIds;
    }
}
